package com.example.demo.lesson;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.demo.concept.Concept;

@Component
public class LessonMappingSupport {

    public UUID contributorId(Lesson lesson) {
        return lesson.getContributor() == null ? null : lesson.getContributor().getContributorId();
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
}
