package me.anjsk.bestoffer.dto;

public class BidRequest {
    private Long bidPrice;

    private Long bidderId;  // 테스트용

    protected BidRequest() {}

    public BidRequest(Long bidPrice) {

        this.bidPrice = bidPrice;
    }

    public Long getBidPrice() { return bidPrice; }
    public Long getBidderId() { return bidderId; }      // 테스트용
}