package com.example.demo.learner;

import org.springframework.stereotype.Component;

import com.example.demo.learner.dto.LearnerPublicDto;

@Component
public class LearnerMapper {

    public LearnerPublicDto toPublicDto(Learner learner) {
        return new LearnerPublicDto(
                learner.getPublicId(),
                learner.getUsername()
        );
    }
}
