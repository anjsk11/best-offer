package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
}
