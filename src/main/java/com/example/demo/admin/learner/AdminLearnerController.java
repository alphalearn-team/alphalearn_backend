package com.example.demo.admin.learner;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.learner.dto.LearnerPublicDto;

@RestController
@RequestMapping("/api/admin/learners")
public class AdminLearnerController {

    private final AdminLearnerFacade adminLearnerFacade;

    public AdminLearnerController(AdminLearnerFacade adminLearnerFacade) {
        this.adminLearnerFacade = adminLearnerFacade;
    }

    @GetMapping
    public List<LearnerPublicDto> getLearners() {
        return adminLearnerFacade.getAllLearners();
    }
}
