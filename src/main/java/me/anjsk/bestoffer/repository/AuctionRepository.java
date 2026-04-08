package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction,Long> {
}
