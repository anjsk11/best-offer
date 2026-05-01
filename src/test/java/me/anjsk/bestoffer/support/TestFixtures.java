package me.anjsk.bestoffer.support;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.domain.enums.UserRole;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(Long id, String email, String nickname) {
        User user = new User(email, "password", nickname, UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Auction auction(Long id, User seller) {
        return auction(id, "Test Auction", "Test Description", 10_000L, LocalDateTime.now().plusDays(1), seller);
    }

    public static Auction auction(Long id, String title, String description, Long startPrice, LocalDateTime endTime, User seller) {
        Auction auction = new Auction(title, description, startPrice, endTime, seller);
        ReflectionTestUtils.setField(auction, "id", id);
        return auction;
    }

    public static Bid bid(Long id, Long bidPrice, Auction auction, User bidder, LocalDateTime bidTime) {
        Bid bid = new Bid(bidPrice, auction, bidder, bidTime);
        ReflectionTestUtils.setField(bid, "id", id);
        return bid;
    }

    public static void setAuctionStatus(Auction auction, AuctionStatus status) {
        ReflectionTestUtils.setField(auction, "status", status);
    }

    public static void setCurrentPrice(Auction auction, Long currentPrice) {
        ReflectionTestUtils.setField(auction, "currentPrice", currentPrice);
    }

    public static void setHighestBidder(Auction auction, User highestBidder) {
        ReflectionTestUtils.setField(auction, "highestBidder", highestBidder);
    }

    public static void setEndTime(Auction auction, LocalDateTime endTime) {
        ReflectionTestUtils.setField(auction, "endTime", endTime);
    }
}
