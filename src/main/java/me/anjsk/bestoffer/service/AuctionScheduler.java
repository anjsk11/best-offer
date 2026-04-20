package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.AuctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    public AuctionScheduler(AuctionRepository auctionRepository, ApplicationEventPublisher eventPublisher) {
        this.auctionRepository = auctionRepository;
        this.eventPublisher = eventPublisher;
    }

    // 초(0) 분(*) 시간(*) 일(*) 월(*) 요일(*) -> 즉, 매 분 0초마다 실행
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Expired Auctions Query");

//        // 종료되어야 할 경매들
//        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ON_SALE, now);

//        // 가져온 경매들을 순회하면서 상태를 COMPLETED로 변경
//        for (Auction auction : expiredAuctions) {
//            auction.markAsCompleted();
//
//            // 최고 입찰자가 존재하면 경매 완료 이벤트 발행
//            if (auction.getHighestBidder() != null) {
//                eventPublisher.publishEvent(new AuctionCompletedEvent(
//                        auction.getId(),
//                        auction.getSeller(),
//                        auction.getHighestBidder(),
//                        auction.getCurrentPrice()
//                ));
//            } else {
//                //  입찰자가 아무도 없어서 유찰된 경우의 로직
//            }
//        }

        // 1. [이벤트 발행용 조회] 낙찰자가 존재하는 만료 경매만 SELECT (인덱스 활용)
        List<Auction> successfulAuctions = auctionRepository.findSuccessfulExpiredAuctions(now);

        // 2. 메모리에서 루프를 돌며 이벤트 발행 (네트워크 I/O 없음)
        for (Auction auction : successfulAuctions) {
            eventPublisher.publishEvent(new AuctionCompletedEvent(
                    auction.getId(),
                    auction.getSeller(),
                    auction.getHighestBidder(),
                    auction.getCurrentPrice()
            ));
        }

        // 벌크 업데이트로 경매 완료 처리
        int updatedCount = auctionRepository.bulkUpdateExpiredAuctions(now);

        stopWatch.stop();
        log.info("만료 경매 조회 쿼리 실행 시간: {} ms", stopWatch.getTotalTimeMillis());
    }
}