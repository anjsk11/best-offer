package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Trade;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeEventListenerTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeEventListener tradeEventListener;

    @Test
    @DisplayName("낙찰 이벤트 수신 성공 - Trade 엔티티가 정상적으로 저장됨")
    void handleAuctionCompleted_Success() {
        // Given: 방금 경매가 끝났다고 가정하고 이벤트 객체 생성
        User seller = new User("seller@test.com", "pass", "판매자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(seller, "id", 1L);

        User buyer = new User("buyer@test.com", "pass", "낙찰자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        Long auctionId = 100L;
        Long finalPrice = 50000L;

        // 스케줄러가 던진 것과 동일한 형태의 이벤트 객체 준비
        AuctionCompletedEvent event = new AuctionCompletedEvent(auctionId, seller, buyer, finalPrice);

        // When: 리스너 메서드 직접 호출 (이벤트 수신 시뮬레이션)
        tradeEventListener.handleAuctionCompleted(event);

        // Then: TradeRepository.save() 가 정확히 1번 호출되었는지 검증
        // 저장 메서드 호출 자체를 확인
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }
}