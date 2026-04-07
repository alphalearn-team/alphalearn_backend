package com.example.demo.game.monthly.repository;

import com.example.demo.game.monthly.GameMonthlyPack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMonthlyPackRepository extends JpaRepository<GameMonthlyPack, Long> {

    Optional<GameMonthlyPack> findByYearMonth(String yearMonth);
}
