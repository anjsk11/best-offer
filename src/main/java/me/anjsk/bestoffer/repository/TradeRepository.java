package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
