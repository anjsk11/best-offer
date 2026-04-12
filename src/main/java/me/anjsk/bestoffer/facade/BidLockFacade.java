package me.anjsk.bestoffer.facade;

import me.anjsk.bestoffer.service.BidService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class BidLockFacade {

    private static final Logger log = LoggerFactory.getLogger(BidLockFacade.class);

    private final RedissonClient redissonClient;
    private final BidService bidService;
    private final RedisTemplate<String, String> redisTemplate; // 💡 Redis 연산을 위해 추가

    // 💡 1. 레디스 비서에게 줄 "입찰 심사 매뉴얼(Lua Script)"
    private static final String LUA_SCRIPT =
            "local info = redis.call('HMGET', KEYS[1], 'status', 'sellerId', 'currentPrice', 'lastBidderId') " +
                    "if info[1] ~= 'OPEN' then return 'ENDED' end " +
                    "if ARGV[1] == info[2] then return 'SELF_BID' end " +
                    "if ARGV[1] == info[4] then return 'CONSECUTIVE' end " +
                    "if tonumber(ARGV[2]) <= tonumber(info[3]) then return 'LOW_PRICE' end " +
                    "redis.call('HMSET', KEYS[1], 'currentPrice', ARGV[2], 'lastBidderId', ARGV[1]) " +
                    "return 'OK'";

    public BidLockFacade(RedissonClient redissonClient, BidService bidService, RedisTemplate<String, String> redisTemplate) {
        this.redissonClient = redissonClient;
        this.bidService = bidService;
        this.redisTemplate = redisTemplate;
    }

    public void placeBidWithLock(Long auctionId, Long bidPrice, Long bidderId, LocalDateTime bidTime) {
        String auctionKey = "auction:" + auctionId + ":info";

        // 💡 2. [1차 방파제] Lua 스크립트로 사전 검증 수행 (락 잡기 전에 실행)
        String result = redisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, String.class),
                Collections.singletonList(auctionKey),
                bidderId.toString(),
                bidPrice.toString()
        );

        // 💡 3. 검증 결과에 따른 예외 처리 (여기서 90%의 부적절한 요청이 걸러짐)
        if (!"OK".equals(result)) {
            handleLuaError(result, auctionId);
        }

        // 💡 4. [2차 관문] 검증 통과된 '진짜'들만 Redisson 락 경쟁 시작
        RLock lock = redissonClient.getLock("auction_lock:" + auctionId);

        try {
            boolean available = lock.tryLock(3, 2, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패 - 대기열이 너무 깁니다. auctionId: {}", auctionId);
                throw new RuntimeException("시스템에 접속자가 많아 입찰이 지연되고 있습니다. 다시 시도해주세요.");
            }

            // 진짜 DB에 저장
            bidService.placeBid(auctionId, bidPrice, bidderId, bidTime);

        } catch (InterruptedException e) {
            log.error("락 획득 중 스레드 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("입찰 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // Lua 스크립트 결과에 따른 예외 메시지 분리
    private void handleLuaError(String result, Long auctionId) {
        switch (result) {
            case "ENDED": throw new RuntimeException("이미 종료된 경매입니다.");
            case "SELF_BID": throw new RuntimeException("본인 경매에는 입찰할 수 없습니다.");
            case "CONSECUTIVE": throw new RuntimeException("연속으로 입찰할 수 없습니다.");
            case "LOW_PRICE": throw new RuntimeException("현재가보다 높은 금액을 입력해주세요.");
            default: throw new RuntimeException("입찰 가능 여부를 확인할 수 없습니다.");
        }
    }
}