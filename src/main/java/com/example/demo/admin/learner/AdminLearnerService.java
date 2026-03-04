package com.example.demo.admin.learner;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.learner.LearnerQueryService;
import com.example.demo.learner.dto.LearnerPublicDto;

@Service
public class AdminLearnerService {

    private final LearnerQueryService learnerQueryService;

    public AdminLearnerService(LearnerQueryService learnerQueryService) {
        this.learnerQueryService = learnerQueryService;
    }

    public List<LearnerPublicDto> getAllLearners() {
        return learnerQueryService.getAllPublicLearners();
    }
}
