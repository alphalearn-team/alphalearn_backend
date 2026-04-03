package com.example.demo.admin.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/weekly-quests")
@Tag(name = "Admin Weekly Quests", description = "Admin-only weekly quest scheduling endpoints")
public class AdminWeeklyQuestController {

    private final AdminWeeklyQuestService adminWeeklyQuestService;

    public AdminWeeklyQuestController(AdminWeeklyQuestService adminWeeklyQuestService) {
        this.adminWeeklyQuestService = adminWeeklyQuestService;
    }

    @GetMapping("/weeks")
    @Operation(summary = "List weekly quests", description = "Returns the current week plus the next 8 weeks by default, including derived placeholder weeks when no row exists yet")
    public List<WeeklyQuestWeekDto> getWeeks(
            @Parameter(description = "Inclusive week start filter in ISO date format. Must be a Sunday when provided.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive week end filter in ISO date format. Must be a Sunday when provided.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminWeeklyQuestService.getWeeks(from, to);
    }

    @GetMapping("/weeks/{weekStartDate}")
    @Operation(summary = "Get weekly quest", description = "Returns one week, or a derived placeholder if the week has not been created yet")
    public WeeklyQuestWeekDto getWeek(@PathVariable String weekStartDate) {
        return adminWeeklyQuestService.getWeek(weekStartDate);
    }

    @PutMapping("/weeks/{weekStartDate}/official")
    @Operation(summary = "Create or update official weekly quest", description = "Upserts the official quest assignment for a target week before the cutoff")
    public WeeklyQuestWeekDto upsertOfficialWeek(
            @PathVariable String weekStartDate,
            @RequestBody UpsertWeeklyQuestAssignmentRequest request,
            @AuthenticationPrincipal SupabaseAuthUser user
    ) {
        return adminWeeklyQuestService.upsertOfficialQuest(
                weekStartDate,
                request,
                user == null ? null : user.userId()
        );
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weekly quest", description = "Returns the current week, using a derived placeholder if no row exists yet")
    public WeeklyQuestWeekDto getCurrentWeek() {
        return adminWeeklyQuestService.getCurrentWeek();
    }

    @GetMapping("/templates")
    @Operation(summary = "List weekly quest templates", description = "Returns active weekly quest templates")
    public List<WeeklyQuestTemplateDto> getTemplates() {
        return adminWeeklyQuestService.getTemplates();
    }
}
