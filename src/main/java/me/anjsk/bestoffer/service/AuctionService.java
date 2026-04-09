package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.exception.InvalidEndTimeException;
import me.anjsk.bestoffer.exception.InvalidPriceException;
import me.anjsk.bestoffer.exception.UserNotFoundException;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.repository.UserRepository;
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
}