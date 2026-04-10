package me.anjsk.bestoffer.domain;

import jakarta.persistence.*;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
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
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // 한 seller가 여러 개의 경매를 진행할 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

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

    // Getter
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getStartPrice() { return startPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public User getSeller() { return seller; }
}
