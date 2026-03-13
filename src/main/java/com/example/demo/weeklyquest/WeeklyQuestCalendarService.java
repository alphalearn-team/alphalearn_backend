package com.example.demo.weeklyquest;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WeeklyQuestCalendarService {

    private final Clock clock;
    private final ZoneId zoneId;

    public WeeklyQuestCalendarService(Clock clock, ZoneId weeklyQuestZoneId) {
        this.clock = clock;
        this.zoneId = weeklyQuestZoneId;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock).atZoneSameInstant(zoneId).toOffsetDateTime();
    }

    public OffsetDateTime weekStartAt(LocalDate targetWeekStartDate) {
        if (targetWeekStartDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target week is required");
        }
        if (targetWeekStartDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target week must start on Sunday");
        }
        return targetWeekStartDate.atStartOfDay(zoneId).toOffsetDateTime();
    }

    public OffsetDateTime setupDeadlineAt(OffsetDateTime weekStartAt) {
        return weekStartAt.minusDays(7);
    }

    public OffsetDateTime currentWeekStartAt() {
        ZonedDateTime zonedNow = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        LocalDate currentDate = zonedNow.toLocalDate();
        int daysFromSunday = currentDate.getDayOfWeek().getValue() % 7;
        return currentDate.minusDays(daysFromSunday).atStartOfDay(zoneId).toOffsetDateTime();
    }

    public OffsetDateTime nextSchedulableWeekStartAt() {
        return currentWeekStartAt().plusDays(14);
    }

    public boolean isEditable(OffsetDateTime weekStartAt) {
        return now().isBefore(setupDeadlineAt(weekStartAt));
    }

    public OffsetDateTime parseWeekStartDate(String weekStartDate) {
        try {
            LocalDate parsed = LocalDate.parse(weekStartDate);
            return weekStartAt(parsed);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weekStartDate must be an ISO date");
        }
    }

    public LocalDate localDate(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(zoneId).toLocalDate();
    }

    public OffsetDateTime atStartOfDay(LocalDate date) {
        return ZonedDateTime.of(LocalDateTime.of(date, LocalTime.MIDNIGHT), zoneId).toOffsetDateTime();
    }
}
