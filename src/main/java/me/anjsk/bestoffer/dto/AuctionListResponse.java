package me.anjsk.bestoffer.dto;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import java.time.LocalDateTime;

public class AuctionListResponse {
    private Long id;
    private String title;
    private Long currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public AuctionListResponse(Auction auction) {
        this.id = auction.getId();
        this.title = auction.getTitle();
        this.currentPrice = auction.getCurrentPrice();
        this.endTime = auction.getEndTime();
        this.status = auction.getStatus();
    }

    public Long id() { return id; }
    public String title() { return title; }
    public Long currentPrice() { return currentPrice; }
    public LocalDateTime endTime() { return endTime; }
    public AuctionStatus status() { return status; }
}