package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.dto.AuctionDetailResponse;
import me.anjsk.bestoffer.dto.AuctionListResponse;
import me.anjsk.bestoffer.dto.AuctionUpdateRequest;
import me.anjsk.bestoffer.exception.*;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public AuctionService(AuctionRepository auctionRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    public Long createAuction(AuctionCreateRequest request, Long sellerId) {
        // 1. 판매자 조회
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new UserNotFoundException());

        // 2. 마감 시간 검증
        if (request.getEndTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new InvalidEndTimeException();
        }

        // 3. 시작가 검증
        if (request.getStartPrice() < 0) {
            throw new InvalidPriceException();
        }

        // 4. 경매 생성
        Auction auction = new Auction(
                request.getTitle(),
                request.getDescription(),
                request.getStartPrice(),
                request.getEndTime(),
                seller
        );

        return auctionRepository.save(auction).getId();
    }

    // 경매 수정
    public void updateAuction(Long auctionId, AuctionUpdateRequest request, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(AuctionNotFoundException::new);

        // 요청한 유저가 판매자인지 확인
        if (!auction.isSeller(userId)) {
            throw new UnauthorizedAccessException();
        }

        // Dirty Checking
        auction.updateInfo(request.getTitle(), request.getDescription());
    }

    // 경매 삭제 (소프트 삭제)
    public void deleteAuction(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(AuctionNotFoundException::new);

        if (!auction.isSeller(userId)) {
            throw new UnauthorizedAccessException();
        }

        auction.markAsDeleted();
    }

    // 단건 상세 조회
    public AuctionDetailResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(AuctionNotFoundException::new);

        return new AuctionDetailResponse(auction);
    }

    // 다건 목록 페이징 조회 (최신순 등은 컨트롤러에서 결정)
    public Page<AuctionListResponse> getAuctions(Pageable pageable) {
        // auctionRepository.findAll()은 Pageable을 받아 Page<Auction>을 반환
        // map()을 이용해 엔티티를 DTO로 변환해 Page<AuctionListResponse>으로
        return auctionRepository.findAll(pageable)
                .map(auction -> new AuctionListResponse(auction));
    }
}