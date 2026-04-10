package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.dto.AuctionUpdateRequest;
import me.anjsk.bestoffer.exception.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import me.anjsk.bestoffer.dto.AuctionDetailResponse;
import me.anjsk.bestoffer.dto.AuctionListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    private User testSeller;
    private final Long SELLER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;
    private final Long AUCTION_ID = 100L;

    @BeforeEach
    void setUp() {

        testSeller = new User("test@test.com", "password", "판매자", UserRole.ROLE_USER);
        // setId()가 없으므로 ReflectionTestUtils를 이용해 강제로 ID 주입
        ReflectionTestUtils.setField(testSeller, "id", SELLER_ID);
    }

    @Test
    @DisplayName("경매 등록 성공")
    void createAuction_Success() {
        // Given
        // 여유롭게 2시간 후로 설정
        LocalDateTime validEndTime = LocalDateTime.now().plusHours(2);
        AuctionCreateRequest request = new AuctionCreateRequest("맥북 프로", "A급", 1000000L, validEndTime);

        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(testSeller));

        Auction savedAuction = new Auction(
                request.getTitle(), request.getDescription(),
                request.getStartPrice(), request.getEndTime(), testSeller
        );
        ReflectionTestUtils.setField(savedAuction, "id", 100L); // DB에서 100번으로 저장됐다고 가정

        given(auctionRepository.save(any(Auction.class))).willReturn(savedAuction);

        // When
        Long auctionId = auctionService.createAuction(request, SELLER_ID);

        // Then
        assertEquals(100L, auctionId); // 리턴된 ID가 100L인지 검증
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    @DisplayName("경매 등록 실패 - 존재하지 않는 사용자")
    void createAuction_Fail_UserNotFound() {
        // Given
        LocalDateTime validEndTime = LocalDateTime.now().plusHours(2);
        AuctionCreateRequest request = new AuctionCreateRequest("맥북 프로", "A급", 1000000L, validEndTime);

        given(userRepository.findById(SELLER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            auctionService.createAuction(request, SELLER_ID);
        });

        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 등록 실패 - 마감 시간이 최소 허용 시간보다 짧음")
    void createAuction_Fail_InvalidEndTime() {
        // Given
        LocalDateTime invalidEndTime = LocalDateTime.now().plusMinutes(30);
        AuctionCreateRequest request = new AuctionCreateRequest("맥북 프로", "A급", 1000000L, invalidEndTime);

        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(testSeller));

        // When & Then
        assertThrows(InvalidEndTimeException.class, () -> {
            auctionService.createAuction(request, SELLER_ID);
        });

        verify(auctionRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 등록 실패 - 시작 가격이 0원 미만(음수)")
    void createAuction_Fail_InvalidPrice() {
        // Given
        LocalDateTime validEndTime = LocalDateTime.now().plusHours(2);
        AuctionCreateRequest request = new AuctionCreateRequest("맥북 프로", "A급", -500L, validEndTime);

        given(userRepository.findById(SELLER_ID)).willReturn(Optional.of(testSeller));

        // When & Then
        assertThrows(InvalidPriceException.class, () -> {
            auctionService.createAuction(request, SELLER_ID);
        });

        verify(auctionRepository, never()).save(any());
    }

    // 경매 수정 (Update) 테스트
    @Test
    @DisplayName("경매 수정 성공")
    void updateAuction_Success() {
        // Given
        Auction testAuction = new Auction("원래 제목", "원래 설명", 10000L, LocalDateTime.now().plusDays(1), testSeller);
        ReflectionTestUtils.setField(testAuction, "id", AUCTION_ID);

        AuctionUpdateRequest request = new AuctionUpdateRequest("수정된 제목", "수정된 설명");
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(testAuction));

        // When
        auctionService.updateAuction(AUCTION_ID, request, SELLER_ID);

        // Then
        // JPA의 더티 체킹을 믿고 엔티티의 값이 잘 바뀌었는지 직접 확인
        assertEquals("수정된 제목", testAuction.getTitle());
        assertEquals("수정된 설명", testAuction.getDescription());
    }

    @Test
    @DisplayName("경매 수정 실패 - 작성자가 아님")
    void updateAuction_Fail_Unauthorized() {
        // Given
        Auction testAuction = new Auction("원래 제목", "원래 설명", 10000L, LocalDateTime.now().plusDays(1), testSeller);
        ReflectionTestUtils.setField(testAuction, "id", AUCTION_ID);

        AuctionUpdateRequest request = new AuctionUpdateRequest("수정된 제목", "수정된 설명");
        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(testAuction));

        // When & Then
        assertThrows(UnauthorizedAccessException.class, () -> {
            auctionService.updateAuction(AUCTION_ID, request, OTHER_USER_ID); // 다른 유저 ID 전달
        });

        // 예외가 터졌으므로 원본 데이터가 보호되었는지 확인
        assertEquals("원래 제목", testAuction.getTitle());
    }

    // 경매 삭제 (Delete) 테스트
    @Test
    @DisplayName("경매 삭제 성공 - 상태가 DELETED로 변경됨")
    void deleteAuction_Success() {
        // Given
        Auction testAuction = new Auction("원래 제목", "원래 설명", 10000L, LocalDateTime.now().plusDays(1), testSeller);
        ReflectionTestUtils.setField(testAuction, "id", AUCTION_ID);

        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(testAuction));

        // When
        auctionService.deleteAuction(AUCTION_ID, SELLER_ID);

        // Then
        assertEquals(AuctionStatus.DELETED, testAuction.getStatus());
    }

    @Test
    @DisplayName("경매 삭제 실패 - 작성자가 아님 (403 Forbidden)")
    void deleteAuction_Fail_Unauthorized() {
        // Given
        Auction testAuction = new Auction("원래 제목", "원래 설명", 10000L, LocalDateTime.now().plusDays(1), testSeller);
        ReflectionTestUtils.setField(testAuction, "id", AUCTION_ID);

        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(testAuction));

        // When & Then
        assertThrows(UnauthorizedAccessException.class, () -> {
            auctionService.deleteAuction(AUCTION_ID, OTHER_USER_ID); // 다른 유저 ID 전달
        });

        // 예외가 터졌으므로 원본 상태(ON_SALE)가 보호되었는지 확인
        assertEquals(AuctionStatus.ON_SALE, testAuction.getStatus());
    }

    @Test
    @DisplayName("경매 단건 조회 성공")
    void getAuction_Success() {
        // Given
        Long auctionId = 1L;
        Auction auction = new Auction("맥북 프로", "A급", 1000000L,
                LocalDateTime.now().plusHours(2), testSeller);
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.ON_SALE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

        User bidder = new User("bidder@test.com", "pass", "입찰자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(bidder, "id", 2L);

        Bid mockBid = new Bid(15000L, auction, bidder, LocalDateTime.now());
        ReflectionTestUtils.setField(mockBid, "id", 1L);

        // 새로 만든 JOIN FETCH 쿼리 메서드가 호출될 때 mockBid 리스트를 반환하도록 설정
        given(bidRepository.findBidsWithBidderByAuctionId(auctionId))
                .willReturn(List.of(mockBid));

        // When
        AuctionDetailResponse response = auctionService.getAuction(auctionId);

        // Then
        assertEquals(auctionId, response.getId());
        assertEquals("맥북 프로", response.getTitle());
        assertEquals("A급", response.getDescription());
        assertEquals(1000000L, response.getStartPrice());
        assertEquals("판매자", response.getSellerNickname());

        // 입찰 내역이 잘 매핑되어 들어왔는가?
        assertEquals(1, response.getBids().size());
        assertEquals(15000L, response.getBids().get(0).getBidPrice());
        assertEquals("입찰자", response.getBids().get(0).getBidderNickname());

        verify(auctionRepository, times(1)).findById(auctionId);
        verify(bidRepository, times(1)).findBidsWithBidderByAuctionId(auctionId);
    }

    @Test
    @DisplayName("경매 상세 조회 성공 - 입찰 내역이 없는 경우 (빈 리스트 반환)")
    void getAuction_Success_NoBids() {
        // Given
        Auction testAuction = new Auction("원래 제목", "원래 설명", 10000L, LocalDateTime.now().plusDays(1), testSeller);
        ReflectionTestUtils.setField(testAuction, "id", AUCTION_ID);

        given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(testAuction));

        // 입찰 내역이 없으므로 빈 리스트 반환
        given(bidRepository.findBidsWithBidderByAuctionId(AUCTION_ID))
                .willReturn(Collections.emptyList());

        // When
        AuctionDetailResponse response = auctionService.getAuction(AUCTION_ID);

        // Then
        assertEquals(AUCTION_ID, response.getId());
        assertEquals(0, response.getBids().size()); // 널(null)이 아니라 빈 리스트(size=0)인지 확인
    }

    @Test
    @DisplayName("경매 단건 조회 실패 - 존재하지 않는 경매")
    void getAuction_Fail_AuctionNotFound() {
        // Given
        Long auctionId = 999L;
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // When & Then
        assertThrows(AuctionNotFoundException.class, () -> {
            auctionService.getAuction(auctionId);
        });

        verify(auctionRepository, times(1)).findById(auctionId);
    }

    @Test
    @DisplayName("경매 목록 페이징 조회 성공")
    void getAuctions_Success() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        Auction auction1 = new Auction("맥북 프로", "A급", 1000000L,
                LocalDateTime.now().plusHours(2), testSeller);
        ReflectionTestUtils.setField(auction1, "id", 2L);
        ReflectionTestUtils.setField(auction1, "status", AuctionStatus.ON_SALE);

        Auction auction2 = new Auction("아이패드", "S급", 500000L,
                LocalDateTime.now().plusHours(3), testSeller);
        ReflectionTestUtils.setField(auction2, "id", 1L);
        ReflectionTestUtils.setField(auction2, "status", AuctionStatus.ON_SALE);

        List<Auction> auctions = List.of(auction1, auction2);
        Page<Auction> auctionPage = new PageImpl<>(auctions, pageable, auctions.size());

        given(auctionRepository.findAll(pageable)).willReturn(auctionPage);

        // When
        Page<AuctionListResponse> response = auctionService.getAuctions(pageable);

        // Then
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getContent().size());
        assertEquals("맥북 프로", response.getContent().get(0).title());
        assertEquals("아이패드", response.getContent().get(1).title());
        verify(auctionRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("경매 목록 페이징 조회 - 결과 없음")
    void getAuctions_EmptyResult() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        Page<Auction> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(auctionRepository.findAll(pageable)).willReturn(emptyPage);

        // When
        Page<AuctionListResponse> response = auctionService.getAuctions(pageable);

        // Then
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getContent().size());
        verify(auctionRepository, times(1)).findAll(pageable);
    }
}