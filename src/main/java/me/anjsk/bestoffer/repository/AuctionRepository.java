package me.anjsk.bestoffer.repository;

import jakarta.persistence.LockModeType;
import me.anjsk.bestoffer.domain.Auction;
import me.anjsk.bestoffer.domain.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction,Long> {
    // ON_SALE 상태이고, 마감 시간이 현재보다 이전인 경매 찾기
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);

    // 기존 findById 대신 사용할 비관적 락 전용 메서드!
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithPessimisticLock(@Param("id") Long id);
}
