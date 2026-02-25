package com.example.demo.learner;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.learner.dto.LearnerPublicDto;

@Service
public class LearnerQueryService {
    
    private final LearnerRepository learnerRepository;
    private final LearnerMapper learnerMapper;

    public LearnerQueryService(LearnerRepository learnerRepository, LearnerMapper learnerMapper) {
        this.learnerRepository = learnerRepository;
        this.learnerMapper = learnerMapper;
    }

    public List<Learner> getAllLearners() {
        return learnerRepository.findAll();
    }

    public List<LearnerPublicDto> getAllPublicLearners() {
        return learnerRepository.findAll().stream()
                .map(learnerMapper::toPublicDto)
                .toList();
    }
}
