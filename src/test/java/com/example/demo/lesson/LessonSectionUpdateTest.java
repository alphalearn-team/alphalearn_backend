package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.contributor.Contributor;
import com.example.demo.lesson.dto.CreateLessonSectionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonSectionUpdateTest {

    @Mock
    private LessonSectionRepository lessonSectionRepository;

    private LessonSectionService lessonSectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lessonSectionService = new LessonSectionService(lessonSectionRepository, objectMapper);
    }

    @Test
    void replaceSectionsWithExistingSectionPublicIdUpdatesSection() {
        Lesson lesson = createLessonWithId(1);
        UUID existingSectionId = UUID.randomUUID();

        // Existing section in database
        LessonSection existingSection = new LessonSection(
                lesson,
                (short) 0,
                SectionType.TEXT,
                "Old Title",
                objectMapper.valueToTree(Map.of("html", "<p>Old content</p>")),
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().minusDays(1)
        );
        try {
            java.lang.reflect.Field field = LessonSection.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(existingSection, existingSectionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set publicId", e);
        }

        // Request with existing sectionPublicId
        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        existingSectionId,  // Providing existing section ID
                        "text",
                        "Updated Title",
                        Map.of("html", "<p>Updated content</p>")
                )
        );

        when(lessonSectionRepository.findByPublicId(existingSectionId)).thenReturn(Optional.of(existingSection));
        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.replaceSectionsForLesson(lesson, requests);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Updated Title");
        assertThat(result.get(0).getPublicId()).isEqualTo(existingSectionId);  // ID preserved
        verify(lessonSectionRepository).saveAll(any());
    }

    @Test
    void replaceSectionsWithNullSectionPublicIdCreatesNewSection() {
        Lesson lesson = createLessonWithId(1);

        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        null,  // No sectionPublicId = new section
                        "text",
                        "New Section",
                        Map.of("html", "<p>New content</p>")
                )
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.replaceSectionsForLesson(lesson, requests);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("New Section");
        verify(lessonSectionRepository).saveAll(any());
    }

    @Test
    void replaceSectionsDeletesOrphanedSections() {
        Lesson lesson = createLessonWithId(1);
        UUID section1Id = UUID.randomUUID();

        // Two existing sections
        LessonSection existingSection1 = createSectionWithId(lesson, section1Id, 0, "Section 1");
        // Section 2 exists in DB but not in request (will be deleted)

        // Request only includes section1
        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        section1Id,
                        "text",
                        "Updated Section 1",
                        Map.of("html", "<p>Content</p>")
                )
        );

        when(lessonSectionRepository.findByPublicId(section1Id)).thenReturn(Optional.of(existingSection1));
        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        lessonSectionService.replaceSectionsForLesson(lesson, requests);

        // Verify ALL sections were deleted (including orphaned section2)
        verify(lessonSectionRepository).deleteByLesson_LessonId(1);
    }

    @Test
    void replaceSectionsWithMixOfNewAndExistingSections() {
        Lesson lesson = createLessonWithId(1);
        UUID existingSectionId = UUID.randomUUID();

        LessonSection existingSection = createSectionWithId(lesson, existingSectionId, 0, "Existing Section");

        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        existingSectionId,
                        "text",
                        "Updated Existing",
                        Map.of("html", "<p>Updated</p>")
                ),
                new CreateLessonSectionRequest(
                        null,  // New section
                        "definition",
                        null,
                        Map.of("term", "New Term", "definition", "New Definition")
                )
        );

        when(lessonSectionRepository.findByPublicId(existingSectionId)).thenReturn(Optional.of(existingSection));
        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.replaceSectionsForLesson(lesson, requests);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPublicId()).isEqualTo(existingSectionId);
        assertThat(result.get(0).getOrderIndex()).isEqualTo((short) 0);
        assertThat(result.get(1).getOrderIndex()).isEqualTo((short) 1);
    }

    @Test
    void replaceSectionsWithInvalidSectionPublicIdThrowsException() {
        Lesson lesson = createLessonWithId(1);
        UUID invalidId = UUID.randomUUID();

        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        invalidId,
                        "text",
                        "Title",
                        Map.of("html", "<p>Content</p>")
                )
        );

        when(lessonSectionRepository.findByPublicId(invalidId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.replaceSectionsForLesson(lesson, requests)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).contains("Section with ID " + invalidId + " not found");
    }

    @Test
    void replaceSectionsWithSectionFromDifferentLessonThrowsException() {
        Lesson lesson1 = createLessonWithId(1);
        Lesson lesson2 = createLessonWithId(2);
        UUID sectionId = UUID.randomUUID();

        // Section belongs to lesson2, but we're trying to update lesson1
        LessonSection sectionFromDifferentLesson = createSectionWithId(lesson2, sectionId, 0, "Section");

        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(
                        sectionId,
                        "text",
                        "Title",
                        Map.of("html", "<p>Content</p>")
                )
        );

        when(lessonSectionRepository.findByPublicId(sectionId)).thenReturn(Optional.of(sectionFromDifferentLesson));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.replaceSectionsForLesson(lesson1, requests)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("does not belong to this lesson");
    }

    private Lesson createLessonWithId(Integer lessonId) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson");
        lesson.setContent(null);
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        lesson.setCreatedAt(OffsetDateTime.now());

        Contributor contributor = new Contributor();
        contributor.setContributorId(UUID.randomUUID());
        lesson.setContributor(contributor);

        try {
            java.lang.reflect.Field field = Lesson.class.getDeclaredField("lessonId");
            field.setAccessible(true);
            field.set(lesson, lessonId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set lessonId", e);
        }
        return lesson;
    }

    private LessonSection createSectionWithId(Lesson lesson, UUID publicId, int orderIndex, String title) {
        LessonSection section = new LessonSection(
                lesson,
                (short) orderIndex,
                SectionType.TEXT,
                title,
                objectMapper.valueToTree(Map.of("html", "<p>Content</p>")),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        try {
            java.lang.reflect.Field field = LessonSection.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(section, publicId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set publicId", e);
        }
        return section;
    }
}
