package com.example.demo.game.imposter.monthly.repository;

import com.example.demo.game.imposter.monthly.ImposterMonthlyPackWeeklyFeature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImposterMonthlyPackWeeklyFeatureRepository extends JpaRepository<ImposterMonthlyPackWeeklyFeature, Long> {

    List<ImposterMonthlyPackWeeklyFeature> findByPack_IdOrderByWeekSlotAsc(Long packId);

    void deleteByPack_Id(Long packId);
}
