package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Trade;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.TradeRepository;
import me.anjsk.bestoffer.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        User seller = TestFixtures.user(1L, "seller@test.com", "seller");
        User buyer = TestFixtures.user(2L, "buyer@test.com", "buyer");
        AuctionCompletedEvent event = new AuctionCompletedEvent(100L, seller, buyer, 50_000L);
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);

        tradeEventListener.handleAuctionCompleted(event);

        verify(tradeRepository).save(tradeCaptor.capture());
        Trade savedTrade = tradeCaptor.getValue();
        assertAll(
                () -> assertEquals(event.auctionId(), savedTrade.getAuctionId()),
                () -> assertEquals(event.seller(), savedTrade.getSeller()),
                () -> assertEquals(event.buyer(), savedTrade.getBuyer()),
                () -> assertEquals(event.finalPrice(), savedTrade.getFinalPrice())
        );
    }
}
