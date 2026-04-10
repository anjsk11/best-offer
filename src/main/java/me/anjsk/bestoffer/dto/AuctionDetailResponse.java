package me.anjsk.bestoffer.dto;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Long startPrice;
    private Long currentPrice;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String sellerNickname;
    private List<BidHistoryDto> bids;

    protected AuctionDetailResponse() {}

    public AuctionDetailResponse(Auction auction, List<Bid> bidEntities) {
        this.id = auction.getId();
        this.title = auction.getTitle();
        this.description = auction.getDescription();
        this.startPrice = auction.getStartPrice();
        this.currentPrice = auction.getCurrentPrice();
        this.endTime = auction.getEndTime();
        this.status = auction.getStatus();
        this.sellerNickname = auction.getSeller().getNickname(); // 지연 로딩 발생 지점
        this.bids = bidEntities.stream()
                .map(BidHistoryDto::new)
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getStartPrice() { return startPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public String getSellerNickname() { return sellerNickname; }
    public List<BidHistoryDto> getBids() { return bids; }
}