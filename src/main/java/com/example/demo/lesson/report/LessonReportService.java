package com.example.demo.lesson.report;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.SupabaseAuthUser;
import com.example.demo.lesson.Lesson;
import com.example.demo.lesson.read.LessonLookupService;
import com.example.demo.lesson.report.dto.CreateLessonReportRequest;
import com.example.demo.lesson.report.dto.LessonReportResponseDto;

@Service
public class LessonReportService {

    private final LessonReportRepository lessonReportRepository;
    private final LessonLookupService lessonLookupService;

    public LessonReportService(
            LessonReportRepository lessonReportRepository,
            LessonLookupService lessonLookupService
    ) {
        this.lessonReportRepository = lessonReportRepository;
        this.lessonLookupService = lessonLookupService;
    }

    @Transactional
    public LessonReportResponseDto createReport(CreateLessonReportRequest request, SupabaseAuthUser user) {
        if (request == null || request.lessonId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lessonId is required");
        }

        UUID reporterUserId = requireReporterUserId(user);
        String reason = trimToNull(request.reason());
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason is required");
        }

        Lesson lesson = lessonLookupService.findPublicByPublicIdOrThrow(request.lessonId());
        UUID ownerUserId = lesson.getContributor() == null ? null : lesson.getContributor().getContributorId();
        if (ownerUserId != null && ownerUserId.equals(reporterUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lesson owners cannot report their own lessons");
        }

        if (lessonReportRepository.existsByLesson_LessonIdAndReporterUserId(lesson.getLessonId(), reporterUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this lesson");
        }

        LessonReport report = new LessonReport();
        report.setLesson(lesson);
        report.setReporterUserId(reporterUserId);
        report.setReason(reason);

        LessonReport saved = lessonReportRepository.save(report);
        return new LessonReportResponseDto(saved.getPublicId(), lesson.getPublicId(), saved.getCreatedAt());
    }

    private UUID requireReporterUserId(SupabaseAuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user required");
        }
        if (!user.isLearner() && !user.isContributor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Learner or contributor access required");
        }
        return user.userId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
