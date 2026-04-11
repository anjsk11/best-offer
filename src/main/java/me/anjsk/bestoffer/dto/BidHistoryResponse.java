package me.anjsk.bestoffer.dto;

import me.anjsk.bestoffer.domain.Bid;
import java.time.LocalDateTime;

public record BidHistoryResponse(
        Long bidId,
        String bidderNickname,
        Long bidPrice,
        LocalDateTime bidTime
) {
    public static BidHistoryResponse from(Bid bid) {
        return new BidHistoryResponse(
                bid.getId(),
                bid.getBidder().getNickname(), // 지연 로딩 발생 지점! (Repository에서 해결할 예정)
                bid.getBidPrice(),
                bid.getBidTime()
        );
    }
}