package com.example.demo.game.admin;

import com.example.demo.game.admin.dto.AdminGameMonthlyPackDto;
import com.example.demo.game.admin.dto.UpsertAdminGameMonthlyPackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/games")
@Tag(name = "Admin Game Monthly Packs", description = "Admin endpoints for monthly imposter game concept packs")
public class AdminGameMonthlyPackController {

    private final AdminGameMonthlyPackService adminGameMonthlyPackService;

    public AdminGameMonthlyPackController(AdminGameMonthlyPackService adminGameMonthlyPackService) {
        this.adminGameMonthlyPackService = adminGameMonthlyPackService;
    }

    @GetMapping("/monthly-packs")
    @Operation(summary = "Get monthly pack by selector", description = "Returns monthly pack by yearMonth or supported scope CURRENT")
    public AdminGameMonthlyPackDto getMonthlyPack(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String scope
    ) {
        boolean hasYearMonth = yearMonth != null && !yearMonth.isBlank();
        boolean hasScope = scope != null && !scope.isBlank();
        if (hasYearMonth == hasScope) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Specify exactly one selector: yearMonth or scope"
            );
        }

        if (hasYearMonth) {
            return adminGameMonthlyPackService.getMonthlyPack(yearMonth.trim());
        }

        String normalizedScope = scope.trim().toUpperCase();
        if (!"CURRENT".equals(normalizedScope)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "scope must be CURRENT");
        }
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
