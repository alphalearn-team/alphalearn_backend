package com.example.demo.me.weeklyquest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/weekly-quest")
@Tag(name = "My Weekly Quest", description = "Learner-facing current weekly quest endpoint")
public class MeWeeklyQuestController {

    private final LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService;

    public MeWeeklyQuestController(LearnerWeeklyQuestQueryService learnerWeeklyQuestQueryService) {
        this.learnerWeeklyQuestQueryService = learnerWeeklyQuestQueryService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weekly quest", description = "Returns the current active weekly concept and quest for the authenticated learner, or null when unavailable")
    public LearnerCurrentWeeklyQuestDto getCurrentWeeklyQuest() {
        return learnerWeeklyQuestQueryService.getCurrentWeeklyQuest().orElse(null);
    }
}
