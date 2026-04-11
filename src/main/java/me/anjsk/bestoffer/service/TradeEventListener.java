package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Trade;
import me.anjsk.bestoffer.event.AuctionCompletedEvent;
import me.anjsk.bestoffer.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradeEventListener.class);
    private final TradeRepository tradeRepository;

    public TradeEventListener(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    // 스케줄러의 경매 종료 업데이트가 완전히 커밋된 후에만 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도의 독립된 트랜잭션으로 저장
    public void handleAuctionCompleted(AuctionCompletedEvent event) {
        log.info("낙찰 이벤트 수신! [경매 ID: {}] 거래 데이터 생성 중...", event.auctionId());

        Trade trade = new Trade(
                event.auctionId(),
                event.seller(),
                event.buyer(),
                event.finalPrice()
        );

        tradeRepository.save(trade);
        log.info("거래(Trade) 생성 완료!");
    }
}