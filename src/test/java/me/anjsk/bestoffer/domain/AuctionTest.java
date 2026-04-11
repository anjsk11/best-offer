package me.anjsk.bestoffer.domain;

import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.exception.AuctionClosedException;
import me.anjsk.bestoffer.exception.LowBidPriceException;
import me.anjsk.bestoffer.exception.SelfBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionTest {

    private User seller;
    private User bidder;
    private Auction auction;
    private final Long SELLER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // 판매자 세팅
        seller = new User("seller@test.com", "pass", "판매자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(seller, "id", SELLER_ID);

        bidder = new User("bidder@test.com", "pass", "입찰자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(bidder, "id", OTHER_USER_ID);

        // 내일 마감이고, 시작가가 10,000원인 정상 경매 세팅
        auction = new Auction("테스트 경매", "설명", 10000L, LocalDateTime.now().plusDays(1), seller);
    }

    @Test
    @DisplayName("입찰 성공 - 현재가가 정상적으로 갱신됨")
    void updateCurrentPrice_Success() {
        // When: 다른 사용자가 15,000원을 입찰
        auction.updateCurrentPrice(15000L, bidder, LocalDateTime.now());

        // Then: 현재가가 15,000원으로 바뀌어야 함
        assertEquals(15000L, auction.getCurrentPrice());
        assertEquals(OTHER_USER_ID, auction.getHighestBidder().getId());
    }

    @Test
    @DisplayName("입찰 실패 - 본인의 경매에 입찰 시도")
    void updateCurrentPrice_Fail_SelfBid() {
        // When & Then
        assertThrows(SelfBidException.class, () -> {
            auction.updateCurrentPrice(15000L, seller, LocalDateTime.now());
        });
    }

    @Test
    @DisplayName("입찰 실패 - 현재가보다 낮거나 같은 금액")
    void updateCurrentPrice_Fail_LowPrice() {
        // When & Then
        assertThrows(LowBidPriceException.class, () -> {
            // 현재가가 10,000원인데 9,000원 입찰 시도
            auction.updateCurrentPrice(9000L, bidder, LocalDateTime.now());
        });

        assertThrows(LowBidPriceException.class, () -> {
            // 현재가와 동일한 10,000원 입찰 시도
            auction.updateCurrentPrice(10000L, bidder, LocalDateTime.now());
        });
    }

    @Test
    @DisplayName("입찰 실패 - 이미 마감 시간이 지난 경우")
    void updateCurrentPrice_Fail_TimeExpired() {
        // Given: 마감 시간을 어제 날짜로 강제 조작
        ReflectionTestUtils.setField(auction, "endTime", LocalDateTime.now().minusDays(1));

        // When & Then
        assertThrows(AuctionClosedException.class, () -> {
            auction.updateCurrentPrice(15000L, bidder, LocalDateTime.now());
        });
    }

    @Test
    @DisplayName("입찰 실패 - 상태가 ON_SALE이 아닌 경우")
    void updateCurrentPrice_Fail_NotOnSale() {
        // Given: 누군가(시스템 등)에 의해 경매가 취소/완료 처리됨
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.COMPLETED);

        // When & Then
        assertThrows(AuctionClosedException.class, () -> {
            auction.updateCurrentPrice(15000L, bidder, LocalDateTime.now());
        });
    }
}