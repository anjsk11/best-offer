package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        User seller = TestFixtures.user(1L, "seller@test.com", "seller");
        bidder = TestFixtures.user(2L, "bidder@test.com", "bidder");
        auction = TestFixtures.auction(100L, "Expired Auction", "Description", 10_000L,
                LocalDateTime.now().minusDays(1), seller);
    }

    @Test
    @DisplayName("낙찰자가 있는 경우 상태 변경 및 이벤트 발행 성공")
    void closeExpiredAuctions_WithWinner_PublishesEvent() {
        TestFixtures.setHighestBidder(auction, bidder);
        given(auctionRepository.findSuccessfulExpiredAuctions(any(LocalDateTime.class)))
                .willReturn(List.of(auction));

        auctionScheduler.closeExpiredAuctions();

        verify(eventPublisher).publishEvent(any(AuctionCompletedEvent.class));
        verify(auctionRepository).bulkUpdateExpiredAuctions(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("낙찰자가 없는 경우 이벤트는 발행하지 않지만 벌크 업데이트는 실행")
    void closeExpiredAuctions_WithoutWinner_NoEvent() {
        given(auctionRepository.findSuccessfulExpiredAuctions(any(LocalDateTime.class)))
                .willReturn(List.of());

        auctionScheduler.closeExpiredAuctions();

        verify(eventPublisher, never()).publishEvent(any());
        verify(auctionRepository).bulkUpdateExpiredAuctions(any(LocalDateTime.class));
    }
}
