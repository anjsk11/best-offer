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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class BidLockFacade {

    private static final Logger log = LoggerFactory.getLogger(BidLockFacade.class);

    private final RedissonClient redissonClient;
    private final BidService bidService;
    private final RedisTemplate<String, String> redisTemplate;

    // 💡 1. LUA 스크립트 수정: 데이터가 없을 때 'NOT_FOUND'를 반환하는 방어 코드 추가
    private static final String LUA_SCRIPT =
            "local info = redis.call('HMGET', KEYS[1], 'status', 'sellerId', 'currentPrice', 'highestBidderId') " +
                    "if not info[1] then return 'NOT_FOUND' end " + // <-- 이 부분이 없어서 계속 ENDED 에러가 났던 것입니다.
                    "if info[1] ~= 'ON_SALE' then return 'ENDED' end " +
                    "if ARGV[1] == info[2] then return 'SELF_BID' end " +
                    "if ARGV[1] == info[4] then return 'CONSECUTIVE' end " +
                    "if tonumber(ARGV[2]) <= tonumber(info[3]) then return 'LOW_PRICE' end " +
                    "redis.call('HMSET', KEYS[1], 'currentPrice', ARGV[2], 'highestBidderId', ARGV[1]) " +
                    "return 'OK'";

    public BidLockFacade(RedissonClient redissonClient, BidService bidService, RedisTemplate<String, String> redisTemplate) {
        this.redissonClient = redissonClient;
        this.bidService = bidService;
        this.redisTemplate = redisTemplate;
    }

    public void placeBidWithLock(Long auctionId, Long bidPrice, Long bidderId, LocalDateTime bidTime) {
        String auctionKey = "auction:" + auctionId + ":info";

        // 💡 2. Lua 스크립트 1차 실행
        String result = executeLuaScript(auctionKey, bidderId, bidPrice);

        // 💡 3. 캐시가 비어있다면? DB에서 가져와서 레디스에 채우고 다시 실행 (Cache-Aside 패턴)
        if ("NOT_FOUND".equals(result)) {
            log.info("Redis 캐시 미스 발생. DB에서 경매 정보를 로드합니다. auctionId: {}", auctionId);
            warmUpCache(auctionId); // 캐시 채우기

            result = executeLuaScript(auctionKey, bidderId, bidPrice); // 스크립트 2차(재)실행
        }

        // 💡 4. 검증 실패 시 즉시 에러 발생 (DB로 안 넘어감)
        if (!"OK".equals(result)) {
            handleLuaError(result, auctionId);
        }

        // 💡 5. 통과된 요청만 분산 락 획득 후 DB 업데이트 진행
        RLock lock = redissonClient.getLock("auction_lock:" + auctionId);

        try {
            boolean available = lock.tryLock(3, 2, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패 - 대기열이 너무 깁니다. auctionId: {}", auctionId);
                throw new RuntimeException("시스템에 접속자가 많아 입찰이 지연되고 있습니다. 다시 시도해주세요.");
            }

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

    // Lua 스크립트 실행을 깔끔하게 분리한 헬퍼 메서드
    private String executeLuaScript(String auctionKey, Long bidderId, Long bidPrice) {
        return redisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, String.class),
                Collections.singletonList(auctionKey),
                bidderId.toString(),
                bidPrice.toString()
        );
    }

    // 💡 핵심: 레디스에 데이터가 없을 때 DB에서 조회해 캐시를 채워주는 메서드
    private void warmUpCache(Long auctionId) {
        // TODO: 실제 프로젝트의 AuctionRepository 나 Service 를 통해 DB에서 경매 정보를 조회해야 합니다.
        // Auction auction = auctionService.findById(auctionId);
        // 아래는 DB에서 값을 정상적으로 가져왔다고 가정한 임시 하드코딩 예시입니다.

        Map<String, String> auctionInfo = new HashMap<>();
        auctionInfo.put("status", "ON_SALE");       // auction.getStatus().name()
        auctionInfo.put("sellerId", "40");          // auction.getSellerId().toString()
        auctionInfo.put("currentPrice", "1000");    // auction.getCurrentPrice().toString()
        auctionInfo.put("highestBidderId", "0");    // auction.getHighestBidderId() != null ? ... : "0"

        String auctionKey = "auction:" + auctionId + ":info";
        redisTemplate.opsForHash().putAll(auctionKey, auctionInfo);
        // 필요하다면 만료 시간 설정 (예: 1시간)
        // redisTemplate.expire(auctionKey, 1, TimeUnit.HOURS);
    }

    private void handleLuaError(String result, Long auctionId) {
        switch (result) {
            case "ENDED": throw new RuntimeException("이미 종료된 경매입니다.");
            case "SELF_BID": throw new RuntimeException("본인 경매에는 입찰할 수 없습니다.");
            case "CONSECUTIVE": throw new RuntimeException("연속으로 입찰할 수 없습니다.");
            case "LOW_PRICE": throw new RuntimeException("현재가보다 높은 금액을 입력해주세요.");
            case "NOT_FOUND": throw new RuntimeException("경매 정보를 찾을 수 없습니다.");
            default: throw new RuntimeException("입찰 가능 여부를 확인할 수 없습니다.");
        }
    }
}