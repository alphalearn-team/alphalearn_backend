package com.example.demo.lesson;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LessonModerationWorkflowService {

    private final LessonRepository lessonRepository;

    public LessonModerationWorkflowService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public Lesson submitForReview(Lesson lesson) {
        lesson.setLessonModerationStatus(LessonModerationStatus.PENDING);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson unpublish(Lesson lesson) {
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson approve(Lesson lesson) {
        requirePending(lesson, "approved");
        lesson.setLessonModerationStatus(LessonModerationStatus.APPROVED);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson reject(Lesson lesson) {
        requirePending(lesson, "rejected");
        lesson.setLessonModerationStatus(LessonModerationStatus.REJECTED);
        return lessonRepository.save(lesson);
    }

    private void requirePending(Lesson lesson, String actionWord) {
        if (lesson.getLessonModerationStatus() != LessonModerationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PENDING lessons can be " + actionWord + "."
            );
        }
    }
}
