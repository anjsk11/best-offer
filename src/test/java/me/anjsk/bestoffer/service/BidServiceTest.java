package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.exception.AuctionNotFoundException;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.repository.BidRepository;
import me.anjsk.bestoffer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock private BidRepository bidRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BidService bidService;

    private User seller;
    private User bidder;
    private Auction auction;

    private final Long SELLER_ID = 1L;
    private final Long BIDDER_ID = 2L;
    private final Long AUCTION_ID = 100L;

    @BeforeEach
    void setUp() {
        seller = new User("seller@test.com", "pass", "판매자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(seller, "id", SELLER_ID);

        bidder = new User("bidder@test.com", "pass", "입찰자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(bidder, "id", BIDDER_ID);

        auction = new Auction("테스트 경매", "설명", 10000L, LocalDateTime.now().plusDays(1), seller);
        ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
    }

    @Test
    @DisplayName("입찰 성공")
    void placeBid_Success() {
        // Given
        Long bidPrice = 15000L;
        given(auctionRepository.findByIdWithPessimisticLock(AUCTION_ID)).willReturn(Optional.of(auction));
        given(userRepository.findById(BIDDER_ID)).willReturn(Optional.of(bidder));

        Bid savedBid = new Bid(bidPrice, auction, bidder, LocalDateTime.now());
        ReflectionTestUtils.setField(savedBid, "id", 500L); // 저장된 입찰 ID는 500이라고 가정
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // When
        Long resultId = bidService.placeBid(AUCTION_ID, bidPrice, BIDDER_ID, LocalDateTime.now());

        // Then
        assertEquals(500L, resultId); // 제대로 된 ID 반환 검증
        assertEquals(15000L, auction.getCurrentPrice()); // 경매 가격이 15,000원으로 갱신되었는지 검증
        verify(bidRepository, times(1)).save(any(Bid.class)); // 저장이 1회 호출되었는지 검증
    }

    @Test
    @DisplayName("입찰 실패 - 존재하지 않는 경매")
    void placeBid_Fail_AuctionNotFound() {
        // Given
        given(auctionRepository.findByIdWithPessimisticLock(AUCTION_ID)).willReturn(Optional.empty());

        // When & Then
        assertThrows(AuctionNotFoundException.class, () -> {
            bidService.placeBid(AUCTION_ID, 15000L, BIDDER_ID, LocalDateTime.now());
        });

        // 예외가 터지면 사용자 조회나 입찰 저장이 호출되지 않아야 함
        verify(userRepository, never()).findById(any());
        verify(bidRepository, never()).save(any());
    }
}