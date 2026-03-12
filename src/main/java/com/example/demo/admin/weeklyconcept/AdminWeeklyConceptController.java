package com.example.demo.admin.weeklyconcept;

import java.time.LocalDate;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.weeklyconcept.dto.WeeklyConceptUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.weeklyconcept.dto.WeeklyConceptResponse;

@RestController
@RequestMapping("/api/admin/weekly-concepts")
@Tag(name = "Admin Weekly Concepts", description = "Admin-only weekly concept management endpoints")
public class AdminWeeklyConceptController {

    private final AdminWeeklyConceptService adminWeeklyConceptService;

    public AdminWeeklyConceptController(AdminWeeklyConceptService adminWeeklyConceptService) {
        this.adminWeeklyConceptService = adminWeeklyConceptService;
    }

    @GetMapping("/{weekStartDate}")
    @Operation(summary = "Get weekly concept by week", description = "Returns the weekly concept for the provided week start date")
    public WeeklyConceptResponse getWeeklyConceptByWeekStartDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate
    ) {
        return adminWeeklyConceptService.getByWeekStartDate(weekStartDate);
    }

    @PutMapping("/{weekStartDate}")
    @Operation(summary = "Set or update weekly concept", description = "Creates or updates the weekly concept for the provided week start date")
    public WeeklyConceptResponse upsertWeeklyConcept(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate,
            @Valid @RequestBody WeeklyConceptUpsertRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return adminWeeklyConceptService.upsertByWeekStartDate(weekStartDate, request, user);
    }
}
