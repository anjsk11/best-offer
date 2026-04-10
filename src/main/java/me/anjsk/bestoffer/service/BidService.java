package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.Bid;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.exception.AuctionNotFoundException;
import me.anjsk.bestoffer.exception.UserNotFoundException;
import me.anjsk.bestoffer.repository.AuctionRepository;
import me.anjsk.bestoffer.repository.BidRepository;
import me.anjsk.bestoffer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public BidService(BidRepository bidRepository, AuctionRepository auctionRepository, UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    public Long placeBid(Long auctionId, Long bidPrice, Long bidderId, LocalDateTime bidTime) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(AuctionNotFoundException::new);
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(UserNotFoundException::new);

        // 비즈니스 로직 검증 및 현재가 갱신
        auction.updateCurrentPrice(bidPrice, bidderId, bidTime);

        // 입찰 기록(Bid) 생성 및 저장
        Bid bid = new Bid(bidPrice, auction, bidder, bidTime);
        return bidRepository.save(bid).getId();
    }
}