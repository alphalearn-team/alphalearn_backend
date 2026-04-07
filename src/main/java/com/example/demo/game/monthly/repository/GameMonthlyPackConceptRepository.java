package com.example.demo.game.monthly.repository;

import com.example.demo.game.monthly.GameMonthlyPackConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMonthlyPackConceptRepository extends JpaRepository<GameMonthlyPackConcept, Long> {

    List<GameMonthlyPackConcept> findByPack_IdOrderBySlotIndexAsc(Long packId);

    void deleteByPack_Id(Long packId);
}
