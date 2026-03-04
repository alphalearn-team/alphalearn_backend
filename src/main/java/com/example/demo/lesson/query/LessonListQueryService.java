package com.example.demo.lesson.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.lesson.Lesson;

@Service
public class LessonListQueryService {

    private final LessonListQueryRepository lessonListQueryRepository;

    public LessonListQueryService(
            LessonListQueryRepository lessonListQueryRepository
    ) {
        this.lessonListQueryRepository = lessonListQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<Lesson> findLessons(LessonListCriteria criteria) {
        return lessonListQueryRepository.findByCriteria(criteria);
    }
}
