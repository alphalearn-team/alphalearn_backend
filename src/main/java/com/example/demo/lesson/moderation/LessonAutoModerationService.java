package com.example.demo.lesson.moderation;

import com.example.demo.lesson.Lesson;

public interface LessonAutoModerationService {

    LessonModerationResult moderate(Lesson lesson);
}
