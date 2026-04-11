package me.anjsk.bestoffer.domain;

import jakarta.persistence.*;
import me.anjsk.bestoffer.domain.enums.PaymentStatus;
import me.anjsk.bestoffer.domain.enums.ShippingStatus;


@Entity
@Table(name = "trades") // ORDER는 예약어이므로 trades 사용
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 객체(Auction) 참조가 아닌 단순 ID만 저장 (느슨한 결합)
    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID; // 초기값: 결제 대기

    @Enumerated(EnumType.STRING)
    private ShippingStatus shippingStatus = ShippingStatus.PREPARING; // 초기값: 배송 준비

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    protected Trade() {}

    // 생성자
    public Trade(Long auctionId, User seller, User buyer, Long finalPrice) {
        this.auctionId = auctionId;
        this.seller = seller;
        this.buyer = buyer;
        this.finalPrice = finalPrice;
    }

    // getter
    public Long getId() { return id; }
    public Long getAuctionId() { return auctionId; }
    public Long getFinalPrice() { return finalPrice; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public ShippingStatus getShippingStatus() { return shippingStatus; }
    public User getSeller() { return seller; }
    public User getBuyer() { return buyer; }
}