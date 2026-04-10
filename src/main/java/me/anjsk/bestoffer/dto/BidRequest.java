package me.anjsk.bestoffer.dto;

public class BidRequest {
    private Long bidPrice;

    protected BidRequest() {}

    public BidRequest(Long bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Long getBidPrice() { return bidPrice; }
}