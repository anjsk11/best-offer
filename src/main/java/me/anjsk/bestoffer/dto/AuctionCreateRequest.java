package me.anjsk.bestoffer.dto;

import java.time.LocalDateTime;

public class AuctionCreateRequest {
    private String title;
    private String description;
    private Long startPrice;
    private LocalDateTime endTime;

    protected AuctionCreateRequest() {}

    public AuctionCreateRequest(String title, String description, Long startPrice, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.endTime = endTime;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getStartPrice() { return startPrice; }
    public LocalDateTime getEndTime() { return endTime; }
}