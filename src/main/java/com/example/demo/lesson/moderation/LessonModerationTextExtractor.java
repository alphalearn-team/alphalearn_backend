package com.example.demo.lesson.moderation;

import org.springframework.stereotype.Component;

import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.LessonSection;

@Component
public class LessonModerationTextExtractor {

    public String extract(Lesson lesson) {
        StringBuilder sb = new StringBuilder();

        if (lesson.getTitle() != null && !lesson.getTitle().isBlank()) {
            sb.append(lesson.getTitle()).append("\n");
        }

        if (lesson.getContent() != null) {
            sb.append(lesson.getContent()).append("\n");
        }

        if (lesson.getSections() != null) {
            for (LessonSection section : lesson.getSections()) {
                if (section.getTitle() != null && !section.getTitle().isBlank()) {
                    sb.append(section.getTitle()).append("\n");
                }
                if (section.getContent() != null) {
                    sb.append(section.getContent()).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }
}
