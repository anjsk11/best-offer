package me.anjsk.bestoffer.facade;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.exception.*;
import me.anjsk.bestoffer.repository.AuctionRepository;
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
    private final AuctionRepository auctionRepository;

    // 캐시를 보고 입찰 예비 필터링, 데이터가 없을 때 'NOT_FOUND'를 반환하는 방어 코드 추가
    private static final String LUA_SCRIPT =
            "local info = redis.call('HMGET', KEYS[1], 'status', 'sellerId', 'currentPrice', 'highestBidderId') " +
                    "if not info[1] then return 'NOT_FOUND' end " +
                    "if info[1] ~= 'ON_SALE' then return 'ENDED' end " +
                    "if ARGV[1] == info[2] then return 'SELF_BID' end " +
                    "if ARGV[1] == info[4] then return 'CONSECUTIVE' end " +
                    "if tonumber(ARGV[2]) <= tonumber(info[3]) then return 'LOW_PRICE' end " +
                    "redis.call('HMSET', KEYS[1], 'currentPrice', ARGV[2], 'highestBidderId', ARGV[1]) " +
                    "return 'OK'";

    public BidLockFacade(RedissonClient redissonClient, BidService bidService, RedisTemplate<String, String> redisTemplate, AuctionRepository auctionRepository) {
        this.redissonClient = redissonClient;
        this.bidService = bidService;
        this.redisTemplate = redisTemplate;
        this.auctionRepository = auctionRepository;
    }

    public void placeBidWithLock(Long auctionId, Long bidPrice, Long bidderId, LocalDateTime bidTime) {
        String auctionKey = "auction:" + auctionId + ":info";

        // Lua 스크립트 1차 실행
        String result = executeLuaScript(auctionKey, bidderId, bidPrice);

        // 캐시가 비어있다면? DB에서 가져와서 레디스에 채우고 다시 실행 (Cache-Aside 패턴)
        if ("NOT_FOUND".equals(result)) {
            log.info("Redis 캐시 미스 발생. DB에서 경매 정보를 로드합니다. auctionId: {}", auctionId);
            warmUpCache(auctionId); // 캐시 채우기

            result = executeLuaScript(auctionKey, bidderId, bidPrice); // 스크립트 2차(재)실행
        }

        // 검증 실패 시 즉시 에러 발생 (DB로 안 넘어감)
        if (!"OK".equals(result)) {
            handleLuaError(result, auctionId);
        }

        // 통과된 요청만 분산 락 획득 후 DB 업데이트 진행
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

    // DB 연동형 Cache Warm-up
    private void warmUpCache(Long auctionId) {
        // DB에서 경매 엔티티 조회 (없으면 예외 발생)
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException());

        // Redis에 넣을 Map 생성
        Map<String, String> auctionInfo = new HashMap<>();

        // 엔티티의 실제 값을 꺼내서 문자열로 변환 후 Map에 담기
        auctionInfo.put("status", auction.getStatus().name());
        auctionInfo.put("sellerId", String.valueOf(auction.getSeller().getId()));
        auctionInfo.put("currentPrice", String.valueOf(auction.getCurrentPrice()));

        // 핵심: 첫 입찰이라 DB에 최고입찰자가 null인 경우, 레디스에는 "0"으로 저장
        String highestBidder = (auction.getHighestBidder() != null)
                ? String.valueOf(auction.getHighestBidder().getId())
                : "0";
        auctionInfo.put("highestBidderId", highestBidder);

        // 레디스에 밀어넣기, 경매키로 그 경매에 맞는 경매 정보를 얻을 수 있음
        String auctionKey = "auction:" + auctionId + ":info";
        redisTemplate.opsForHash().putAll(auctionKey, auctionInfo);

        // (선택) 레디스 메모리 낭비를 막기 위해 1시간 뒤 자동 만료 설정
        redisTemplate.expire(auctionKey, 1, TimeUnit.HOURS);

        log.info("DB에서 경매({}) 정보를 성공적으로 Redis에 캐싱했습니다.", auctionId);
    }

    private void handleLuaError(String result, Long auctionId) {
        switch (result) {
            case "ENDED":
                throw new AuctionClosedException();
            case "SELF_BID":
                throw new SelfBidException();
            case "CONSECUTIVE":
                throw new ConsecutiveBidException();
            case "LOW_PRICE":
                throw new LowBidPriceException();
            case "NOT_FOUND":
                throw new AuctionNotFoundException();
            default:
                // 예상치 못한 에러
                throw new RuntimeException("입찰 가능 여부를 확인할 수 없습니다. 코드: " + result);
        }
    }
}