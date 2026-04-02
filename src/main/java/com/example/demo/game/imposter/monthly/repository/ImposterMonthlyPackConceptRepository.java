package com.example.demo.game.imposter.monthly.repository;

import com.example.demo.game.imposter.monthly.ImposterMonthlyPackConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterMonthlyPackConceptRepository extends JpaRepository<ImposterMonthlyPackConcept, Long> {

    List<ImposterMonthlyPackConcept> findByPack_IdOrderBySlotIndexAsc(Long packId);

    void deleteByPack_Id(Long packId);
}
