package com.example.demo.quest.weekly;

import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WeeklyQuestProperties.class)
public class WeeklyQuestConfig {

    @Bean
    public ZoneId weeklyQuestZoneId(WeeklyQuestProperties properties) {
        String configuredZoneId = properties.zoneId();
        if (configuredZoneId == null || configuredZoneId.isBlank()) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(configuredZoneId);
    }
}
