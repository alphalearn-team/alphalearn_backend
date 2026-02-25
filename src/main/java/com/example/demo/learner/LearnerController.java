package com.example.demo.learner;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/learners")
public class LearnerController {
    
    private final LearnerQueryService learnerQueryService;

    public LearnerController(LearnerQueryService learnerQueryService) {
        this.learnerQueryService = learnerQueryService;
    }

    @GetMapping
    public List<Learner> getLearners() {
        return learnerQueryService.getAllLearners();
    }
    
}
