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
        auction.updateCurrentPrice(bidPrice, bidder, bidTime);

        // 강제 쓰기(Flush): 변경된 Auction 정보를 DB에 즉시 반영
        // 이 메서드가 끝나고 Facade에서 락을 풀었을 때, 다음 사람이 최신 가격을 보도록
        auctionRepository.saveAndFlush(auction);

        // 입찰 기록(Bid) 생성 및 저장
        Bid bid = new Bid(bidPrice, auction, bidder, bidTime);
        return bidRepository.save(bid).getId();
    }
}