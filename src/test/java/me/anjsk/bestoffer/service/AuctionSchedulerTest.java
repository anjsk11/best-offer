package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = new User("seller@test.com", "pass", "판매자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(seller, "id", 1L);

        bidder = new User("bidder@test.com", "pass", "입찰자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(bidder, "id", 2L);

        // 어제 마감된 경매 세팅
        auction = new Auction("테스트 경매", "설명", 10000L, LocalDateTime.now().minusDays(1), seller);
        ReflectionTestUtils.setField(auction, "id", 100L);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.ON_SALE);
    }

    @Test
    @DisplayName("낙찰자가 있는 경우 상태 변경 및 이벤트 발행 성공")
    void closeExpiredAuctions_WithWinner_PublishesEvent() {
        // Given: 낙찰자가 있는 상황 세팅
        ReflectionTestUtils.setField(auction, "highestBidder", bidder);
        ReflectionTestUtils.setField(auction, "currentPrice", 15000L);

        given(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ON_SALE), any(LocalDateTime.class)))
                .willReturn(List.of(auction));

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        assertEquals(AuctionStatus.COMPLETED, auction.getStatus()); // 상태가 종료로 바뀌었는지 확인

        // 핵심 검증: 이벤트 퍼블리셔가 '정확히 1번' 호출되었는지 확인
        verify(eventPublisher, times(1)).publishEvent(any(AuctionCompletedEvent.class));
    }

    @Test
    @DisplayName("낙찰자가 없는(유찰) 경우 상태만 변경하고 이벤트 발행 안 함")
    void closeExpiredAuctions_WithoutWinner_NoEventPublished() {
        // Given: 낙찰자가 없는(null) 상황 세팅
        ReflectionTestUtils.setField(auction, "highestBidder", null);

        given(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ON_SALE), any(LocalDateTime.class)))
                .willReturn(List.of(auction));

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        assertEquals(AuctionStatus.COMPLETED, auction.getStatus()); // 상태는 무조건 종료로 바뀌어야 함

        // 💡 핵심 검증: 이벤트 퍼블리셔가 '절대' 호출되지 않았음을 확인 (유찰 처리)
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("마감할 경매가 없는 경우 아무 일도 일어나지 않음")
    void closeExpiredAuctions_NoExpiredAuctions() {
        // Given: 빈 리스트 반환
        given(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ON_SALE), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }
}