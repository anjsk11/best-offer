package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.dto.AuctionDetailResponse;
import me.anjsk.bestoffer.dto.AuctionListResponse;
import me.anjsk.bestoffer.dto.AuctionUpdateRequest;
import me.anjsk.bestoffer.dto.BidHistoryResponse;
import me.anjsk.bestoffer.exception.AuctionNotFoundException;
import me.anjsk.bestoffer.exception.InvalidEndTimeException;
import me.anjsk.bestoffer.exception.InvalidPriceException;
import me.anjsk.bestoffer.exception.UnauthorizedAccessException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    private static final Long SELLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long AUCTION_ID = 100L;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    private User seller;

    @BeforeEach
    void setUp() {
        seller = TestFixtures.user(SELLER_ID, "seller@test.com", "seller");
    }

    @Test
    @DisplayName("경매 등록 성공")
    void createAuction_Success() {
        AuctionCreateRequest request = createRequest(1_000_000L, LocalDateTime.now().plusHours(2));
        Auction savedAuction = TestFixtures.auction(AUCTION_ID, request.getTitle(), request.getDescription(),
                request.getStartPrice(), request.getEndTime(), seller);

        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));
        given(auctionRepository.save(any(Auction.class))).willReturn(savedAuction);

        Long auctionId = auctionService.createAuction(request, SELLER_ID);

        assertEquals(AUCTION_ID, auctionId);
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    @DisplayName("경매 등록 실패 - 존재하지 않는 사용자")
    void createAuction_Fail_UserNotFound() {
        AuctionCreateRequest request = createRequest(1_000_000L, LocalDateTime.now().plusHours(2));
        given(userRepository.findById(SELLER_ID)).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> auctionService.createAuction(request, SELLER_ID));
        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 등록 실패 - 마감 시간이 최소 허용 시간보다 짧음")
    void createAuction_Fail_InvalidEndTime() {
        AuctionCreateRequest request = createRequest(1_000_000L, LocalDateTime.now().minusMinutes(30));
        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));

        assertThrows(InvalidEndTimeException.class, () -> auctionService.createAuction(request, SELLER_ID));
        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 등록 실패 - 시작 가격이 0원 미만(음수)")
    void createAuction_Fail_InvalidPrice() {
        AuctionCreateRequest request = createRequest(-500L, LocalDateTime.now().plusHours(2));
        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(seller));

        assertThrows(InvalidPriceException.class, () -> auctionService.createAuction(request, SELLER_ID));
        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 수정 성공")
    void updateAuction_Success() {
        Auction auction = TestFixtures.auction(AUCTION_ID, seller);
        AuctionUpdateRequest request = new AuctionUpdateRequest("Updated Title", "Updated Description");
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

        auctionService.updateAuction(AUCTION_ID, request, SELLER_ID);

        assertAll(
                () -> assertEquals("Updated Title", auction.getTitle()),
                () -> assertEquals("Updated Description", auction.getDescription())
        );
    }

    @Test
    @DisplayName("경매 수정 실패 - 작성자가 아님")
    void updateAuction_Fail_Unauthorized() {
        Auction auction = TestFixtures.auction(AUCTION_ID, "Original Title", "Original Description",
                10_000L, LocalDateTime.now().plusDays(1), seller);
        AuctionUpdateRequest request = new AuctionUpdateRequest("Updated Title", "Updated Description");
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

        assertThrows(UnauthorizedAccessException.class,
                () -> auctionService.updateAuction(AUCTION_ID, request, OTHER_USER_ID));
        assertEquals("Original Title", auction.getTitle());
    }

    @Test
    @DisplayName("경매 삭제 성공 - 상태가 DELETED로 변경됨")
    void deleteAuction_Success() {
        Auction auction = TestFixtures.auction(AUCTION_ID, seller);
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

        auctionService.deleteAuction(AUCTION_ID, SELLER_ID);

        assertEquals(AuctionStatus.DELETED, auction.getStatus());
    }

    @Test
    @DisplayName("경매 삭제 실패 - 작성자가 아님 (403 Forbidden)")
    void deleteAuction_Fail_Unauthorized() {
        Auction auction = TestFixtures.auction(AUCTION_ID, seller);
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

        assertThrows(UnauthorizedAccessException.class,
                () -> auctionService.deleteAuction(AUCTION_ID, OTHER_USER_ID));
        assertEquals(AuctionStatus.ON_SALE, auction.getStatus());
    }

    @Test
    @DisplayName("경매 상세 조회 성공")
    void getAuction_Success() {
        Auction auction = TestFixtures.auction(AUCTION_ID, "Vintage Camera", "A grade",
                1_000_000L, LocalDateTime.now().plusHours(2), seller);
        TestFixtures.setCurrentPrice(auction, 1_500_000L);
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

        AuctionDetailResponse response = auctionService.getAuction(AUCTION_ID);

        assertAll(
                () -> assertEquals(AUCTION_ID, response.getId()),
                () -> assertEquals("Vintage Camera", response.getTitle()),
                () -> assertEquals("A grade", response.getDescription()),
                () -> assertEquals(1_000_000L, response.getStartPrice()),
                () -> assertEquals(1_500_000L, response.getCurrentPrice()),
                () -> assertEquals("seller", response.getSellerNickname())
        );
        verify(auctionRepository).findById(AUCTION_ID);
    }

    @Test
    @DisplayName("경매 상세 조회 실패 - 존재하지 않는 경매")
    void getAuction_Fail_AuctionNotFound() {
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.empty());

        assertThrows(AuctionNotFoundException.class, () -> auctionService.getAuction(AUCTION_ID));
        verify(auctionRepository).findById(AUCTION_ID);
    }

    @Test
    @DisplayName("경매 입찰 내역 페이징 조회 성공")
    void getBidHistory_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        User bidder = TestFixtures.user(2L, "bidder@test.com", "bidder");
        Auction auction = TestFixtures.auction(AUCTION_ID, seller);
        Bid bid = TestFixtures.bid(1L, 1_500_000L, auction, bidder, LocalDateTime.now());
        Page<Bid> bidPage = new PageImpl<>(List.of(bid), pageable, 1);

        given(auctionRepository.existsById(AUCTION_ID)).willReturn(true);
        given(bidRepository.findBidsByAuctionId(AUCTION_ID, pageable)).willReturn(bidPage);

        Page<BidHistoryResponse> response = auctionService.getBidHistory(AUCTION_ID, pageable);

        assertAll(
                () -> assertEquals(1, response.getTotalElements()),
                () -> assertEquals(1, response.getContent().size()),
                () -> assertEquals(1_500_000L, response.getContent().get(0).bidPrice()),
                () -> assertEquals("bidder", response.getContent().get(0).bidderNickname())
        );
        verify(auctionRepository).existsById(AUCTION_ID);
        verify(bidRepository).findBidsByAuctionId(AUCTION_ID, pageable);
    }

    @Test
    @DisplayName("경매 입찰 내역 페이징 조회 성공 - 아무도 입찰하지 않은 경우 (빈 페이지 반환)")
    void getBidHistory_Success_NoBids() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Bid> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(auctionRepository.existsById(AUCTION_ID)).willReturn(true);
        given(bidRepository.findBidsByAuctionId(AUCTION_ID, pageable)).willReturn(emptyPage);

        Page<BidHistoryResponse> response = auctionService.getBidHistory(AUCTION_ID, pageable);

        assertAll(
                () -> assertEquals(0, response.getTotalElements()),
                () -> assertEquals(0, response.getContent().size())
        );
    }

    @Test
    @DisplayName("경매 입찰 내역 페이징 조회 실패 - 존재하지 않는 경매 ID")
    void getBidHistory_Fail_AuctionNotFound() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(auctionRepository.existsById(AUCTION_ID)).willReturn(false);

        assertThrows(AuctionNotFoundException.class,
                () -> auctionService.getBidHistory(AUCTION_ID, pageable));
        verify(bidRepository, never()).findBidsByAuctionId(anyLong(), any());
    }

    @Test
    @DisplayName("경매 목록 페이징 조회 성공")
    void getAuctions_Success() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Auction first = TestFixtures.auction(2L, "Vintage Camera", "A grade",
                1_000_000L, LocalDateTime.now().plusHours(2), seller);
        Auction second = TestFixtures.auction(1L, "Diamond", "S grade",
                500_000L, LocalDateTime.now().plusHours(3), seller);
        Page<Auction> auctionPage = new PageImpl<>(List.of(first, second), pageable, 2);

        given(auctionRepository.findAll(pageable)).willReturn(auctionPage);

        Page<AuctionListResponse> response = auctionService.getAuctions(pageable);

        assertAll(
                () -> assertEquals(2, response.getTotalElements()),
                () -> assertEquals(1, response.getTotalPages()),
                () -> assertEquals(2, response.getContent().size()),
                () -> assertEquals("Vintage Camera", response.getContent().get(0).getTitle()),
                () -> assertEquals("Diamond", response.getContent().get(1).getTitle())
        );
        verify(auctionRepository).findAll(pageable);
    }

    @Test
    @DisplayName("경매 목록 페이징 조회 - 결과 없음")
    void getAuctions_EmptyResult() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        given(auctionRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AuctionListResponse> response = auctionService.getAuctions(pageable);

        assertAll(
                () -> assertEquals(0, response.getTotalElements()),
                () -> assertEquals(0, response.getTotalPages()),
                () -> assertEquals(0, response.getContent().size())
        );
        verify(auctionRepository).findAll(pageable);
    }

    private AuctionCreateRequest createRequest(Long startPrice, LocalDateTime endTime) {
        return new AuctionCreateRequest("Vintage Camera", "A grade", startPrice, endTime);
    }
}
