package com.example.demo.lesson.read;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonRepository;

@Service
public class LessonLookupService {

    private final LessonRepository lessonRepository;

    public LessonLookupService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public Lesson findByPublicIdOrThrow(UUID lessonPublicId) {
        return lessonRepository.findByPublicId(lessonPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @Transactional(readOnly = true)
    public Lesson findPublicByPublicIdOrThrow(UUID lessonPublicId) {
        return lessonRepository.findPublicByPublicId(lessonPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }
}
