package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // N+1 문제 방지: JOIN FETCH를 사용하여 Bid와 연관된 User(bidder)를 한 번의 쿼리로 (입찰가 내림차순)
    @Query("SELECT b FROM Bid b JOIN FETCH b.bidder WHERE b.auction.id = :auctionId ORDER BY b.bidPrice DESC")
    List<Bid> findBidsWithBidderByAuctionId(@Param("auctionId") Long auctionId);
}
