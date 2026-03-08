package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.contributor.Contributor;
import com.example.demo.lesson.dto.CreateLessonSectionRequest;
import com.example.demo.lesson.dto.LessonSectionDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LessonSectionServiceTest {

    @Mock
    private LessonSectionRepository lessonSectionRepository;

    private LessonSectionService lessonSectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lessonSectionService = new LessonSectionService(lessonSectionRepository, objectMapper);
    }

    @Test
    void createSectionsWithValidTextSection() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "text",
                "Introduction",
                Map.of("html", "<p>This is the introduction</p>")
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, List.of(request));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.TEXT);
        assertThat(result.get(0).getTitle()).isEqualTo("Introduction");
        assertThat(result.get(0).getOrderIndex()).isEqualTo((short) 0);
    }

    @Test
    void createSectionsWithValidExampleSection() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "example",
                "Common Usage",
                Map.of("examples", List.of(
                        Map.of("text", "That's so skibidi!", "context", "Casual conversation"),
                        Map.of("text", "Your outfit is very skibidi")
                ))
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, List.of(request));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.EXAMPLE);
    }

    @Test
    void createSectionsWithValidCalloutSection() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "callout",
                null,
                Map.of(
                        "variant", "info",
                        "title", "Did you know?",
                        "html", "<p>Skibidi comes from the viral series</p>"
                )
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, List.of(request));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.CALLOUT);
    }

    @Test
    void createSectionsWithValidDefinitionSection() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "definition",
                null,
                Map.of(
                        "term", "Skibidi",
                        "pronunciation", "/ˈskɪb.ə.di/",
                        "definition", "A versatile slang term"
                )
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, List.of(request));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.DEFINITION);
    }

    @Test
    void createSectionsWithValidComparisonSection() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "comparison",
                null,
                Map.of("items", List.of(
                        Map.of("label", "Skibidi way", "description", "Modern slang usage"),
                        Map.of("label", "Traditional way", "description", "Standard English")
                ))
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, List.of(request));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.COMPARISON);
    }

    @Test
    void createSectionsWithMultipleSectionsAssignsCorrectOrderIndex() {
        Lesson lesson = createLesson();
        List<CreateLessonSectionRequest> requests = List.of(
                new CreateLessonSectionRequest(null, "text", "First", Map.of("html", "<p>First section</p>")),
                new CreateLessonSectionRequest(null, "text", "Second", Map.of("html", "<p>Second section</p>")),
                new CreateLessonSectionRequest(null, "text", "Third", Map.of("html", "<p>Third section</p>"))
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.createSectionsForLesson(lesson, requests);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getOrderIndex()).isEqualTo((short) 0);
        assertThat(result.get(1).getOrderIndex()).isEqualTo((short) 1);
        assertThat(result.get(2).getOrderIndex()).isEqualTo((short) 2);
    }

    @Test
    void createSectionsWithInvalidSectionTypeFails() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "invalid_type",
                null,
                Map.of("html", "<p>Content</p>")
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.createSectionsForLesson(lesson, List.of(request))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("Invalid sectionType");
    }

    @Test
    void createSectionsWithMissingRequiredFieldFails() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "text",
                null,
                Map.of() // Missing 'html' field
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.createSectionsForLesson(lesson, List.of(request))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("Missing required field 'html'");
    }

    @Test
    void createSectionsWithInvalidCalloutVariantFails() {
        Lesson lesson = createLesson();
        CreateLessonSectionRequest request = new CreateLessonSectionRequest(
                null,
                "callout",
                null,
                Map.of(
                        "variant", "invalid_variant",
                        "html", "<p>Content</p>"
                )
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.createSectionsForLesson(lesson, List.of(request))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("variant");
    }

    @Test
    void createSectionsWithEmptyArrayFails() {
        Lesson lesson = createLesson();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.createSectionsForLesson(lesson, List.of())
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("At least one section is required");
    }

    @Test
    void toSectionDtosConvertsCorrectly() {
        Lesson lesson = createLesson();
        LessonSection section = new LessonSection(
                lesson,
                (short) 0,
                SectionType.TEXT,
                "Test Section",
                objectMapper.valueToTree(Map.of("html", "<p>Content</p>")),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        List<LessonSectionDto> result = lessonSectionService.toSectionDtos(List.of(section));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderIndex()).isEqualTo(0);
        assertThat(result.get(0).sectionType()).isEqualTo("text");
        assertThat(result.get(0).title()).isEqualTo("Test Section");
    }

    @Test
    void replaceSectionsDeletesExistingAndCreatesNew() {
        Lesson lesson = createLessonWithId(1);
        List<CreateLessonSectionRequest> newSections = List.of(
                new CreateLessonSectionRequest(null, "text", "New Section", Map.of("html", "<p>New content</p>"))
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.replaceSectionsForLesson(lesson, newSections);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSectionType()).isEqualTo(SectionType.TEXT);
        assertThat(result.get(0).getTitle()).isEqualTo("New Section");
    }

    @Test
    void replaceSectionsWithMultipleSectionsAssignsCorrectOrderIndex() {
        Lesson lesson = createLessonWithId(1);
        List<CreateLessonSectionRequest> newSections = List.of(
                new CreateLessonSectionRequest(null, "text", "First", Map.of("html", "<p>First</p>")),
                new CreateLessonSectionRequest(null, "definition", null, Map.of("term", "Test", "definition", "A test")),
                new CreateLessonSectionRequest(null, "callout", null, Map.of("variant", "info", "html", "<p>Note</p>"))
        );

        when(lessonSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LessonSection> result = lessonSectionService.replaceSectionsForLesson(lesson, newSections);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getOrderIndex()).isEqualTo((short) 0);
        assertThat(result.get(1).getOrderIndex()).isEqualTo((short) 1);
        assertThat(result.get(2).getOrderIndex()).isEqualTo((short) 2);
    }

    @Test
    void replaceSectionsWithEmptyArrayFails() {
        Lesson lesson = createLessonWithId(1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.replaceSectionsForLesson(lesson, List.of())
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("At least one section is required");
    }

    @Test
    void replaceSectionsWithInvalidSectionTypeFails() {
        Lesson lesson = createLessonWithId(1);
        List<CreateLessonSectionRequest> newSections = List.of(
                new CreateLessonSectionRequest(null, "invalid", null, Map.of("html", "<p>Content</p>"))
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> lessonSectionService.replaceSectionsForLesson(lesson, newSections)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("Invalid sectionType");
    }

    private Lesson createLesson() {
        Lesson lesson = new Lesson();
        lesson.setTitle("Test Lesson");
        lesson.setContent(null);
        lesson.setLessonModerationStatus(LessonModerationStatus.UNPUBLISHED);
        lesson.setCreatedAt(OffsetDateTime.now());
        
        Contributor contributor = new Contributor();
        contributor.setContributorId(UUID.randomUUID());
        lesson.setContributor(contributor);
        
        return lesson;
    }

    private Lesson createLessonWithId(Integer lessonId) {
        Lesson lesson = createLesson();
        try {
            java.lang.reflect.Field field = Lesson.class.getDeclaredField("lessonId");
            field.setAccessible(true);
            field.set(lesson, lessonId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set lessonId", e);
        }
        return lesson;
    }
}
