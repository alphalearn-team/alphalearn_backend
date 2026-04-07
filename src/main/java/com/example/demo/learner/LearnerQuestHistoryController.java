package com.example.demo.learner;

import com.example.demo.quest.learner.QuestHistoryDto;
import com.example.demo.quest.learner.QuestHistoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learners")
@Tag(name = "Learners (Public)", description = "Read-only learner endpoints available to authenticated non-admin users")
public class LearnerQuestHistoryController {

    private final QuestHistoryQueryService questHistoryQueryService;

    public LearnerQuestHistoryController(QuestHistoryQueryService questHistoryQueryService) {
        this.questHistoryQueryService = questHistoryQueryService;
    }

    @GetMapping("/{learnerPublicId}/weekly-quest/history")
    @Operation(
            summary = "Get learner public quest history",
            description = "Returns paginated PUBLIC weekly quest submissions for the selected learner"
    )
    public QuestHistoryDto getPublicQuestHistory(
            @PathVariable UUID learnerPublicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<UUID> weekPublicIds
    ) {
        return questHistoryQueryService.getPublicHistory(learnerPublicId, page, size, weekPublicIds);
    }
}
