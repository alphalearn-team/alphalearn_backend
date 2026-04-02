package com.example.demo.admin.imposter;

import com.example.demo.admin.imposter.dto.AdminImposterMonthlyPackDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/imposter")
@Tag(name = "Admin Imposter Monthly Packs", description = "Admin endpoints for monthly imposter game concept packs")
public class AdminImposterMonthlyPackController {

    private final AdminImposterMonthlyPackService adminImposterMonthlyPackService;

    public AdminImposterMonthlyPackController(AdminImposterMonthlyPackService adminImposterMonthlyPackService) {
        this.adminImposterMonthlyPackService = adminImposterMonthlyPackService;
    }

    @GetMapping("/monthly-packs/{yearMonth}")
    @Operation(summary = "Get monthly imposter pack", description = "Returns one month pack or an exists=false scaffold when the month is not configured")
    public AdminImposterMonthlyPackDto getMonthlyPack(@PathVariable String yearMonth) {
        return adminImposterMonthlyPackService.getMonthlyPack(yearMonth);
    }

    @GetMapping("/monthly-packs/current")
    @Operation(summary = "Get current monthly imposter pack", description = "Returns the current UTC month pack or an exists=false scaffold when the month is not configured")
    public AdminImposterMonthlyPackDto getCurrentMonthlyPack() {
        return adminImposterMonthlyPackService.getCurrentMonthlyPack();
    }
}
