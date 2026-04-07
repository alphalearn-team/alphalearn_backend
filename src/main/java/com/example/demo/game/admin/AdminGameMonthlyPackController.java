package com.example.demo.game.admin;

import com.example.demo.game.admin.dto.AdminGameMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminGameMonthlyPackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/imposter")
@Tag(name = "Admin Game Monthly Packs", description = "Admin endpoints for monthly imposter game concept packs")
public class AdminGameMonthlyPackController {

    private final AdminGameMonthlyPackService adminGameMonthlyPackService;

    public AdminGameMonthlyPackController(AdminGameMonthlyPackService adminGameMonthlyPackService) {
        this.adminGameMonthlyPackService = adminGameMonthlyPackService;
    }

    @GetMapping("/monthly-packs/{yearMonth}")
    @Operation(summary = "Get monthly imposter pack", description = "Returns one month pack or an exists=false scaffold when the month is not configured")
    public AdminGameMonthlyPackDto getMonthlyPack(@PathVariable String yearMonth) {
        return adminGameMonthlyPackService.getMonthlyPack(yearMonth);
    }

    @GetMapping("/monthly-packs/current")
    @Operation(summary = "Get current monthly imposter pack", description = "Returns the current UTC month pack or an exists=false scaffold when the month is not configured")
    public AdminGameMonthlyPackDto getCurrentMonthlyPack() {
        return adminGameMonthlyPackService.getCurrentMonthlyPack();
    }

    @PutMapping("/monthly-packs/{yearMonth}")
    @Operation(summary = "Create or update monthly imposter pack", description = "Upserts a month pack with exactly 20 concepts and 4 weekly featured concept slots")
    public AdminGameMonthlyPackDto upsertMonthlyPack(
            @PathVariable String yearMonth,
            @RequestBody UpsertAdminGameMonthlyPackRequest request
    ) {
        return adminGameMonthlyPackService.upsertMonthlyPack(yearMonth, request);
    }
}
