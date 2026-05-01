package me.anjsk.bestoffer.domain;

import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.exception.AuctionClosedException;
import me.anjsk.bestoffer.exception.LowBidPriceException;
import me.anjsk.bestoffer.exception.SelfBidException;
import me.anjsk.bestoffer.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionTest {

    private static final Long SELLER_ID = 1L;
    private static final Long BIDDER_ID = 2L;

    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = TestFixtures.user(SELLER_ID, "seller@test.com", "seller");
        bidder = TestFixtures.user(BIDDER_ID, "bidder@test.com", "bidder");
        auction = TestFixtures.auction(100L, seller);
    }

    @Test
    @DisplayName("입찰 성공 - 현재가가 정상적으로 갱신됨")
    void updateCurrentPrice_Success() {
        auction.updateCurrentPrice(15_000L, bidder, LocalDateTime.now());

        assertEquals(15_000L, auction.getCurrentPrice());
        assertEquals(BIDDER_ID, auction.getHighestBidder().getId());
    }

    @Test
    @DisplayName("입찰 실패 - 본인의 경매에 입찰 시도")
    void updateCurrentPrice_Fail_SelfBid() {
        assertThrows(SelfBidException.class,
                () -> auction.updateCurrentPrice(15_000L, seller, LocalDateTime.now()));
    }

    @Test
    @DisplayName("입찰 실패 - 현재가보다 낮거나 같은 금액")
    void updateCurrentPrice_Fail_LowPrice() {
        assertThrows(LowBidPriceException.class,
                () -> auction.updateCurrentPrice(9_000L, bidder, LocalDateTime.now()));
        assertThrows(LowBidPriceException.class,
                () -> auction.updateCurrentPrice(10_000L, bidder, LocalDateTime.now()));
    }

    @Test
    @DisplayName("입찰 실패 - 이미 마감 시간이 지난 경우")
    void updateCurrentPrice_Fail_TimeExpired() {
        TestFixtures.setEndTime(auction, LocalDateTime.now().minusDays(1));

        assertThrows(AuctionClosedException.class,
                () -> auction.updateCurrentPrice(15_000L, bidder, LocalDateTime.now()));
    }

    @Test
    @DisplayName("입찰 실패 - 상태가 ON_SALE이 아닌 경우")
    void updateCurrentPrice_Fail_NotOnSale() {
        TestFixtures.setAuctionStatus(auction, AuctionStatus.COMPLETED);

        assertThrows(AuctionClosedException.class,
                () -> auction.updateCurrentPrice(15_000L, bidder, LocalDateTime.now()));
    }
}
