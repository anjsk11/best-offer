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
        // Given
        ReflectionTestUtils.setField(auction, "highestBidder", bidder);
        given(auctionRepository.findSuccessfulExpiredAuctions(any(LocalDateTime.class)))
                .willReturn(List.of(auction));

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        // 1. 이벤트가 발행되었는지 검증
        verify(eventPublisher, times(1)).publishEvent(any(AuctionCompletedEvent.class));

        // 2. 벌크 업데이트 메서드가 호출되었는지 검증 (벌크 연산은 객체 상태를 직접 바꾸지 않으므로 호출 여부가 중요)
        verify(auctionRepository, times(1)).bulkUpdateExpiredAuctions(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("낙찰자가 없는 경우 이벤트는 발행하지 않지만 벌크 업데이트는 실행")
    void closeExpiredAuctions_WithoutWinner_NoEvent() {
        // Given: 낙찰자가 없는 경우, findSuccessfulExpiredAuctions는 빈 리스트를 반환한다고 가정
        given(auctionRepository.findSuccessfulExpiredAuctions(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        // 1. 이벤트는 절대 발행되면 안 됨
        verify(eventPublisher, never()).publishEvent(any());

        // 2. 하지만 유찰된 건들도 상태는 바뀌어야 하므로 벌크 업데이트는 호출되어야 함
        verify(auctionRepository, times(1)).bulkUpdateExpiredAuctions(any(LocalDateTime.class));
    }
}