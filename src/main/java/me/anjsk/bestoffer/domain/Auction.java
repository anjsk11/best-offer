package me.anjsk.bestoffer.domain;

import jakarta.persistence.*;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.exception.AuctionClosedException;
import me.anjsk.bestoffer.exception.ConsecutiveBidException;
import me.anjsk.bestoffer.exception.LowBidPriceException;
import me.anjsk.bestoffer.exception.SelfBidException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@EntityListeners(AuditingEntityListener.class)
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long startPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private AuctionStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    protected Auction() {}

    public Auction(String title, String description, Long startPrice, LocalDateTime endTime, User seller) {
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.endTime = endTime;
        this.seller = seller;
        this.status = AuctionStatus.ON_SALE;
    }

    // 작성자 검증 로직
    public boolean isSeller(Long userId) {
        return this.seller.getId().equals(userId);
    }

    // 경매 정보 수정 (제목과 설명만 수정 가능)
    public void updateInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // 소프트 삭제 처리
    public void markAsDeleted() {
        this.status = AuctionStatus.DELETED;
    }

    // 입찰 발생 시 입찰 가능 여부 판단 후 가격 갱신
    public void updateCurrentPrice(Long bidPrice, User bidder, LocalDateTime bidTime) {
//        // 1. 종료된 경매인지 검증
//        if (this.status != AuctionStatus.ON_SALE || bidTime.isAfter(this.endTime)) {
//            throw new AuctionClosedException();
//        }
//
//        // 2. 본인 입찰 금지
//        if (this.seller.getId().equals(bidder.getId())) {
//            throw new SelfBidException();
//        }
//
//        // 연속 입찰 금지
//        // 최고 입찰자가 존재하고, 그 입찰자의 ID가 지금 입찰하려는 사람의 ID와 같다면 차단
//        if (this.highestBidder != null && this.highestBidder.getId().equals(bidder.getId())) {
//            throw new ConsecutiveBidException();
//        }
//
//        // 3. 가격 검증
//        if (bidPrice <= this.currentPrice) {
//            throw new LowBidPriceException();
//        }

        // 4. 모두 통과했다면 가격 갱신
        this.currentPrice = bidPrice;
        this.highestBidder = bidder;
    }

    // 경매를 종료 상태로 변경
    public void markAsCompleted() {
        this.status = AuctionStatus.COMPLETED;
    }

    // Getter
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getStartPrice() { return startPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public User getSeller() { return seller; }
    public User getHighestBidder() { return highestBidder; }
}
