package me.anjsk.bestoffer.facade;

import me.anjsk.bestoffer.service.BidService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class BidLockFacade {

    private static final Logger log = LoggerFactory.getLogger(BidLockFacade.class);

    private final RedissonClient redissonClient;
    private final BidService bidService;

    public BidLockFacade(RedissonClient redissonClient, BidService bidService) {
        this.redissonClient = redissonClient;
        this.bidService = bidService;
    }

    public void placeBidWithLock(Long auctionId, Long bidPrice, Long bidderId, LocalDateTime bidTime) {
        RLock lock = redissonClient.getLock("auction_lock:" + auctionId);

        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);

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
}