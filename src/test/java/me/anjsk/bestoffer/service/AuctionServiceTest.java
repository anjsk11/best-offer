package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.exception.InvalidEndTimeException;
import me.anjsk.bestoffer.exception.InvalidPriceException;
import me.anjsk.bestoffer.exception.UserNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
}