package com.example.demo.lesson;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LessonLookupService {

    private final LessonRepository lessonRepository;

    public LessonLookupService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public Lesson findByIdOrThrow(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @Transactional(readOnly = true)
    public Lesson findByPublicIdOrThrow(UUID publicId) {
        return lessonRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @Transactional(readOnly = true)
    public Lesson findPublicByIdOrThrow(Integer lessonId) {
        return lessonRepository.findPublicById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @Transactional(readOnly = true)
    public Lesson findPublicByPublicIdOrThrow(UUID publicId) {
        return lessonRepository.findPublicByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }
}
