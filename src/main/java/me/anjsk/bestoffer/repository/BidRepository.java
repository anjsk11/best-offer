package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // Fetch Join 적용: 입찰 내역을 가져올 때 '입찰자(User)' 정보도 한 번의 쿼리로 다 가져옵니다.
    // countQuery는 페이징 처리를 위해 전체 개수를 세는 쿼리
    @Query(value = "SELECT b FROM Bid b JOIN FETCH b.bidder WHERE b.auction.id = :auctionId",
            countQuery = "SELECT count(b) FROM Bid b WHERE b.auction.id = :auctionId")
    Page<Bid> findBidsByAuctionId(@Param("auctionId") Long auctionId, Pageable pageable);
}
