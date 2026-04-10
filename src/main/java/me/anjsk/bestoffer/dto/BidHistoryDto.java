package me.anjsk.bestoffer.dto;

import me.anjsk.bestoffer.domain.Bid;
import java.time.LocalDateTime;

public class BidHistoryDto {
    private String bidderNickname;
    private Long bidPrice;
    private LocalDateTime bidTime;

    protected BidHistoryDto() {}

    public BidHistoryDto(Bid bid) {
        this.bidderNickname = bid.getBidder().getNickname();
        this.bidPrice = bid.getBidPrice();
        this.bidTime = bid.getBidTime();
    }

    // Getter
    public String getBidderNickname() { return bidderNickname; }
    public Long getBidPrice() { return bidPrice; }
    public LocalDateTime getBidTime() { return bidTime; }
}