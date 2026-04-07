package com.example.demo.learner.read;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerMapper;
import com.example.demo.learner.LearnerRepository;
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
