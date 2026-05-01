package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.exception.AuctionNotFoundException;
import me.anjsk.bestoffer.exception.UserNotFoundException;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.repository.BidRepository;
import me.anjsk.bestoffer.repository.UserRepository;
import me.anjsk.bestoffer.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    private static final Long SELLER_ID = 1L;
    private static final Long BIDDER_ID = 2L;
    private static final Long AUCTION_ID = 100L;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BidService bidService;

    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        User seller = TestFixtures.user(SELLER_ID, "seller@test.com", "seller");
        bidder = TestFixtures.user(BIDDER_ID, "bidder@test.com", "bidder");
        auction = TestFixtures.auction(AUCTION_ID, seller);
    }

    @Test
    @DisplayName("입찰 성공")
    void placeBid_Success() {
        LocalDateTime bidTime = LocalDateTime.now();
        Long bidPrice = 15_000L;
        Bid savedBid = TestFixtures.bid(500L, bidPrice, auction, bidder, bidTime);

        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));
        given(userRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        Long resultId = bidService.placeBid(AUCTION_ID, bidPrice, BIDDER_ID, bidTime);

        assertEquals(500L, resultId);
        assertEquals(bidPrice, auction.getCurrentPrice());
        verify(bidRepository).save(any(Bid.class));
    }

    @Test
    @DisplayName("입찰 실패 - 존재하지 않는 경매")
    void placeBid_Fail_AuctionNotFound() {
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.empty());

        assertThrows(AuctionNotFoundException.class,
                () -> bidService.placeBid(AUCTION_ID, 15_000L, BIDDER_ID, LocalDateTime.now()));

        verify(userRepository, never()).findById(any());
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("입찰 실패 - 존재하지 않는 입찰자")
    void placeBid_Fail_UserNotFound() {
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));
        given(userRepository.findById(BIDDER_ID)).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> bidService.placeBid(AUCTION_ID, 15_000L, BIDDER_ID, LocalDateTime.now()));

        verify(bidRepository, never()).save(any());
    }
}
