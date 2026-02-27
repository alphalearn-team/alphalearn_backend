package com.example.demo.admin.learner;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learner.dto.LearnerPublicDto;

@RestController
@RequestMapping("/api/admin/learners")
@Tag(name = "Admin Learners", description = "Admin-only learner listing endpoints")
public class AdminLearnerController {

    private final AdminLearnerFacade adminLearnerFacade;

    public AdminLearnerController(AdminLearnerFacade adminLearnerFacade) {
        this.adminLearnerFacade = adminLearnerFacade;
    }

    @GetMapping
    @Operation(summary = "List learners (admin)", description = "Returns all learner public profiles for administration")
    public List<LearnerPublicDto> getLearners() {
        return adminLearnerFacade.getAllLearners();
    }
}
