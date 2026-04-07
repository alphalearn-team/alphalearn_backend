package com.example.demo.game.monthly.repository;

import com.example.demo.game.monthly.GameMonthlyPackWeeklyFeature;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMonthlyPackWeeklyFeatureRepository extends JpaRepository<GameMonthlyPackWeeklyFeature, Long> {

    List<GameMonthlyPackWeeklyFeature> findByPack_IdOrderByWeekSlotAsc(Long packId);

    Optional<GameMonthlyPackWeeklyFeature> findByPack_IdAndWeekSlot(Long packId, short weekSlot);

    void deleteByPack_Id(Long packId);
}
