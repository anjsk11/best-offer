package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction,Long> {
    // ON_SALE 상태이고, 마감 시간이 현재보다 이전인 경매 찾기
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);

    // [이벤트 발행용 조회] 낙찰자가 있는 만료 경매만 조회 (Fetch Join으로 N+1 완벽 방지)
    @Query("SELECT a FROM Auction a " +
            "JOIN FETCH a.seller " +                // 판매자 정보 한 번에 가져오기
            "JOIN FETCH a.highestBidder " +         // 낙찰자 정보 한 번에 가져오기
            "WHERE a.status = 'ON_SALE' " +
            "AND a.endTime < :now " +
            "AND a.highestBidder IS NOT NULL")      // 유찰된 건은 제외하고 낙찰자가 있는 건만!
    List<Auction> findSuccessfulExpiredAuctions(@Param("now") LocalDateTime now);

    // 벌크 처리로 만료된 경매 완료 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Auction a SET a.status = 'COMPLETED' " +
            "WHERE a.status = 'ON_SALE' AND a.endTime < :now")
    int bulkUpdateExpiredAuctions(@Param("now") LocalDateTime now);

}
