package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction,Long> {
    // ON_SALE 상태이고, 마감 시간이 현재보다 이전인 경매 찾기
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);

}
