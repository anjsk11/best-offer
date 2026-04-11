package me.anjsk.bestoffer.dto;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import java.time.LocalDateTime;


public class AuctionDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Long startPrice;
    private Long currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String sellerNickname;

    protected AuctionDetailResponse() {}

    public AuctionDetailResponse(Auction auction) {
        this.id = auction.getId();
        this.title = auction.getTitle();
        this.description = auction.getDescription();
        this.startPrice = auction.getStartPrice();
        this.currentPrice = auction.getCurrentPrice();
        this.endTime = auction.getEndTime();
        this.status = auction.getStatus();
        this.sellerNickname = auction.getSeller().getNickname(); // 지연 로딩 발생 지점
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getStartPrice() { return startPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public String getSellerNickname() { return sellerNickname; }
}