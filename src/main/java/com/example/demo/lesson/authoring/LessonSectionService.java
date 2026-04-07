package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.dto.CreateLessonSectionRequest;
import com.example.demo.lesson.dto.LessonSectionDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LessonSectionService {

    private final LessonSectionRepository lessonSectionRepository;
    private final ObjectMapper objectMapper;
    private final LessonSectionValidationSupport validationSupport;

    public LessonSectionService(
            LessonSectionRepository lessonSectionRepository,
            ObjectMapper objectMapper
    ) {
        this.lessonSectionRepository = lessonSectionRepository;
        this.objectMapper = objectMapper;
        this.validationSupport = new LessonSectionValidationSupport(lessonSectionRepository, objectMapper);
    }

    @Transactional
    public List<LessonSection> createSectionsForLesson(Lesson lesson, List<CreateLessonSectionRequest> sectionsRequest) {
        validationSupport.ensureSectionsPresent(sectionsRequest);

        List<LessonSection> sections = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 0; i < sectionsRequest.size(); i++) {
            sections.add(validationSupport.toSectionEntity(lesson, sectionsRequest.get(i), i, now));
        }

        return lessonSectionRepository.saveAll(sections);
    }

    @Transactional
    public List<LessonSection> replaceSectionsForLesson(Lesson lesson, List<CreateLessonSectionRequest> sectionsRequest) {
        validationSupport.ensureSectionsPresent(sectionsRequest);
        validationSupport.validateReplaceOwnership(lesson, sectionsRequest);

        OffsetDateTime now = OffsetDateTime.now();
        lessonSectionRepository.deleteByLesson_LessonId(lesson.getLessonId());
        lessonSectionRepository.flush();

        List<LessonSection> newSections = new ArrayList<>();
        for (int i = 0; i < sectionsRequest.size(); i++) {
            newSections.add(validationSupport.toSectionEntity(lesson, sectionsRequest.get(i), i, now));
        }

        return lessonSectionRepository.saveAll(newSections);
    }

    @Transactional(readOnly = true)
    public List<LessonSection> getSectionsForLesson(Lesson lesson) {
        return lessonSectionRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getLessonId());
    }

    public List<LessonSectionDto> toSectionDtos(List<LessonSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }

        return sections.stream()
                .map(section -> new LessonSectionDto(
                        section.getPublicId(),
                        section.getOrderIndex().intValue(),
                        section.getSectionType().name().toLowerCase(),
                        section.getTitle(),
                        objectMapper.convertValue(section.getContent(), Object.class)
                ))
                .toList();
    }
}
