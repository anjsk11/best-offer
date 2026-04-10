package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.repository.AuctionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    @Test
    @DisplayName("자동 마감 스케줄러 성공")
    void closeExpiredAuctions_Success() {
        // Given
        User seller = new User("seller@test.com", "pass", "판매자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(seller, "id", 1L);

        // 마감 시간이 이미 '어제'로 지나버린 경매 2개 생성 (현재 상태: ON_SALE)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        Auction expiredAuction1 = new Auction("경매 1", "설명 1", 10000L, yesterday, seller);
        ReflectionTestUtils.setField(expiredAuction1, "id", 100L);
        ReflectionTestUtils.setField(expiredAuction1, "status", AuctionStatus.ON_SALE);

        Auction expiredAuction2 = new Auction("경매 2", "설명 2", 20000L, yesterday, seller);
        ReflectionTestUtils.setField(expiredAuction2, "id", 101L);
        ReflectionTestUtils.setField(expiredAuction2, "status", AuctionStatus.ON_SALE);

        // Repository가 호출될 때 위 2개의 만료된 경매 리스트를 반환하도록 Mocking
        // 주의: LocalDateTime.now()는 실행될 때마다 미세하게 다르므로 any()로 처리
        given(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ON_SALE), any(LocalDateTime.class)))
                .willReturn(List.of(expiredAuction1, expiredAuction2));

        // When
        auctionScheduler.closeExpiredAuctions();

        // Then
        // 1. 엔티티들의 상태가 ON_SALE에서 COMPLETED로 정확히 변경되었는지 확인
        assertEquals(AuctionStatus.COMPLETED, expiredAuction1.getStatus());
        assertEquals(AuctionStatus.COMPLETED, expiredAuction2.getStatus());

        // 2. Repository의 조회 메서드가 정확히 1번 호출되었는지 검증
        verify(auctionRepository, times(1)).findByStatusAndEndTimeBefore(eq(AuctionStatus.ON_SALE), any(LocalDateTime.class));
    }
}