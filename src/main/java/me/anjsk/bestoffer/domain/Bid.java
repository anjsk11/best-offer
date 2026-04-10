package me.anjsk.bestoffer.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@EntityListeners(AuditingEntityListener.class)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bidPrice;

    // createdDate와 다르게 서버 도착 시간을 기준으로 할 것
    @Column(nullable = false)
    private LocalDateTime bidTime;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    protected Bid() {}

    public Bid(Long bidPrice, Auction auction, User bidder) {
        this.bidPrice = bidPrice;
        this.auction = auction;
        this.bidder = bidder;
        this.bidTime = LocalDateTime.now();
    }

    // Getter
    public Long getId() { return id; }
    public Long getBidPrice() { return bidPrice; }
    public LocalDateTime getBidTime() { return bidTime; }
    public Auction getAuction() { return auction; }
    public User getBidder() { return bidder; }
}