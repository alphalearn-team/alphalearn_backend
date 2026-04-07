package com.example.demo.game.imposter.monthly.repository;

import com.example.demo.game.imposter.monthly.ImposterMonthlyPack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterMonthlyPackRepository extends JpaRepository<ImposterMonthlyPack, Long> {

    Optional<ImposterMonthlyPack> findByYearMonth(String yearMonth);
}
