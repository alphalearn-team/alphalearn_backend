package com.example.demo.admin.weeklyquest;

import com.example.demo.config.SupabaseAuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/weekly-quests")
@Tag(name = "Admin Weekly Quests", description = "Admin-only weekly quest scheduling endpoints")
public class AdminWeeklyQuestController {

    private final AdminWeeklyQuestService adminWeeklyQuestService;

    public AdminWeeklyQuestController(AdminWeeklyQuestService adminWeeklyQuestService) {
        this.adminWeeklyQuestService = adminWeeklyQuestService;
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
}
