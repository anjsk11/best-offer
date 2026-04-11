package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.AuctionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuctionScheduler(AuctionRepository auctionRepository, ApplicationEventPublisher eventPublisher) {
        this.auctionRepository = auctionRepository;
        this.eventPublisher = eventPublisher;
    }

    // 초(0) 분(*) 시간(*) 일(*) 월(*) 요일(*) -> 즉, 매 분 0초마다 실행
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 종료되어야 할 경매들
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.ON_SALE, now);

        // 가져온 경매들을 순회하면서 상태를 COMPLETED로 변경
        for (Auction auction : expiredAuctions) {
            auction.markAsCompleted();

            // 최고 입찰자가 존재하면 경매 완료 이벤트 발행
            if (auction.getHighestBidder() != null) {
                eventPublisher.publishEvent(new AuctionCompletedEvent(
                        auction.getId(),
                        auction.getSeller(),
                        auction.getHighestBidder(),
                        auction.getCurrentPrice()
                ));
            } else {
                //  입찰자가 아무도 없어서 유찰된 경우의 로직
            }
        }
    }
}