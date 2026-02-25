package com.example.demo.learner;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class LearnerQueryService {
    
    private final LearnerRepository learnerRepository;

    public LearnerQueryService(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    public List<Learner> getAllLearners() {
        return learnerRepository.findAll();
    }
}
