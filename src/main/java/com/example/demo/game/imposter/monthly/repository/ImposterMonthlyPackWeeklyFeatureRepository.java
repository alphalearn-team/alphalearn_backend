package com.example.demo.game.imposter.monthly.repository;

import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterMonthlyPackWeeklyFeatureRepository extends JpaRepository<ImposterMonthlyPackWeeklyFeature, Long> {

    List<ImposterMonthlyPackWeeklyFeature> findByPack_IdOrderByWeekSlotAsc(Long packId);

    Optional<ImposterMonthlyPackWeeklyFeature> findByPack_IdAndWeekSlot(Long packId, short weekSlot);

    void deleteByPack_Id(Long packId);
}
