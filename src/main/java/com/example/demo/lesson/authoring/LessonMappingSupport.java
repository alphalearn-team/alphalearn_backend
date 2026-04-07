package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.demo.concept.Concept;
import com.example.demo.lesson.dto.LessonAuthorDto;
import com.example.demo.lesson.dto.LessonConceptSummaryDto;

@Component
public class LessonMappingSupport {

    public UUID contributorId(Lesson lesson) {
        return lesson.getContributor() == null ? null : lesson.getContributor().getContributorId();
    }

    public LessonAuthorDto author(Lesson lesson) {
        if (lesson.getContributor() == null || lesson.getContributor().getLearner() == null) {
            return null;
        }

        return new LessonAuthorDto(
                lesson.getContributor().getLearner().getPublicId(),
                lesson.getContributor().getLearner().getUsername()
        );
    }

    public List<Integer> conceptIds(Lesson lesson) {
        if (lesson.getConcepts() == null || lesson.getConcepts().isEmpty()) {
            return List.of();
        }

        return lesson.getConcepts().stream()
                .map(Concept::getConceptId)
                .sorted()
                .toList();
    }

    public List<UUID> conceptPublicIds(Lesson lesson) {
        if (lesson.getConcepts() == null || lesson.getConcepts().isEmpty()) {
            return List.of();
        }

        return lesson.getConcepts().stream()
                .map(Concept::getPublicId)
                .sorted()
                .toList();
    }

    public List<LessonConceptSummaryDto> conceptSummaries(Lesson lesson) {
        if (lesson.getConcepts() == null || lesson.getConcepts().isEmpty()) {
            return List.of();
        }

        return lesson.getConcepts().stream()
                .sorted(java.util.Comparator.comparing(Concept::getPublicId))
                .map(concept -> new LessonConceptSummaryDto(concept.getPublicId(), concept.getTitle()))
                .toList();
    }
}
