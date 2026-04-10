package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.exception.InvalidEndTimeException;
import me.anjsk.bestoffer.exception.InvalidPriceException;
import me.anjsk.bestoffer.exception.UserNotFoundException;
import me.anjsk.bestoffer.exception.AuctionNotFoundException;
import me.anjsk.bestoffer.repository.AuctionRepository;
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

    @InjectMocks
    private AuctionService auctionService;

    private User testSeller;
    private final Long SELLER_ID = 1L;

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

        // When
        AuctionDetailResponse response = auctionService.getAuction(auctionId);

        // Then
        assertEquals(auctionId, response.getId());
        assertEquals("맥북 프로", response.getTitle());
        assertEquals("A급", response.getDescription());
        assertEquals(1000000L, response.getStartPrice());
        assertEquals("판매자", response.getSellerNickname());
        verify(auctionRepository, times(1)).findById(auctionId);
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