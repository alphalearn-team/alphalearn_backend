package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import com.example.demo.lesson.dto.CreateLessonSectionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LessonSectionValidationSupport {

    private final LessonSectionRepository lessonSectionRepository;
    private final ObjectMapper objectMapper;

    LessonSectionValidationSupport(
            LessonSectionRepository lessonSectionRepository,
            ObjectMapper objectMapper
    ) {
        this.lessonSectionRepository = lessonSectionRepository;
        this.objectMapper = objectMapper;
    }

    void ensureSectionsPresent(List<CreateLessonSectionRequest> sectionsRequest) {
        if (sectionsRequest == null || sectionsRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one section is required");
        }
    }

    void validateReplaceOwnership(Lesson lesson, List<CreateLessonSectionRequest> sectionsRequest) {
        for (CreateLessonSectionRequest sectionRequest : sectionsRequest) {
            if (sectionRequest.sectionPublicId() == null) {
                continue;
            }
            LessonSection existingSection = lessonSectionRepository
                    .findByPublicId(sectionRequest.sectionPublicId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Section with ID " + sectionRequest.sectionPublicId() + " not found"
                    ));
            if (!existingSection.getLesson().getLessonId().equals(lesson.getLessonId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Section with ID " + sectionRequest.sectionPublicId() + " does not belong to this lesson"
                );
            }
        }
    }

    LessonSection toSectionEntity(
            Lesson lesson,
            CreateLessonSectionRequest sectionRequest,
            int index,
            OffsetDateTime now
    ) {
        validateSectionRequest(sectionRequest, index);
        SectionType sectionType = parseSectionType(sectionRequest.sectionType());
        JsonNode content = objectMapper.valueToTree(sectionRequest.content());
        validateSectionContent(sectionType, content, index);

        UUID preservedPublicId = sectionRequest.sectionPublicId();
        if (preservedPublicId != null) {
            return new LessonSection(
                    preservedPublicId,
                    lesson,
                    (short) index,
                    sectionType,
                    sectionRequest.title(),
                    content,
                    now,
                    now
            );
        }
        return new LessonSection(
                lesson,
                (short) index,
                sectionType,
                sectionRequest.title(),
                content,
                now,
                now
        );
    }

    private void validateSectionRequest(CreateLessonSectionRequest request, int index) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section at index " + index + " is null"
            );
        }

        if (request.sectionType() == null || request.sectionType().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section at index " + index + ": sectionType is required"
            );
        }

        if (request.content() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section at index " + index + ": content is required"
            );
        }

        if (request.title() != null && request.title().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section at index " + index + ": title exceeds maximum length of 500 characters"
            );
        }
    }

    private SectionType parseSectionType(String sectionTypeStr) {
        try {
            return SectionType.valueOf(sectionTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sectionType '" + sectionTypeStr + "'. Must be one of: text, example, callout, definition, comparison"
            );
        }
    }

    private void validateSectionContent(SectionType sectionType, JsonNode content, int index) {
        String errorPrefix = "Section at index " + index + " (type=" + sectionType.name().toLowerCase() + "): ";
        try {
            switch (sectionType) {
                case TEXT -> validateTextContent(content, errorPrefix);
                case EXAMPLE -> validateExampleContent(content, errorPrefix);
                case CALLOUT -> validateCalloutContent(content, errorPrefix);
                case DEFINITION -> validateDefinitionContent(content, errorPrefix);
                case COMPARISON -> validateComparisonContent(content, errorPrefix);
            }
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorPrefix + "Invalid content structure: " + e.getMessage()
            );
        }
    }

    private void validateTextContent(JsonNode content, String errorPrefix) {
        if (!content.has("html")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'html'");
        }

        JsonNode html = content.get("html");
        if (!html.isTextual() || html.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'html' must be a non-empty string");
        }
    }

    private void validateExampleContent(JsonNode content, String errorPrefix) {
        if (!content.has("examples")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'examples'");
        }

        JsonNode examples = content.get("examples");
        if (!examples.isArray() || examples.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'examples' must be a non-empty array");
        }

        for (int i = 0; i < examples.size(); i++) {
            JsonNode example = examples.get(i);
            if (!example.has("text") || !example.get("text").isTextual() || example.get("text").asText().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        errorPrefix + "Example at index " + i + " must have a non-empty 'text' field"
                );
            }
        }
    }

    private void validateCalloutContent(JsonNode content, String errorPrefix) {
        if (!content.has("variant")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'variant'");
        }

        String variant = content.get("variant").asText();
        if (!List.of("info", "warning", "tip", "note").contains(variant)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorPrefix + "Field 'variant' must be one of: info, warning, tip, note"
            );
        }

        if (!content.has("html")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'html'");
        }

        JsonNode html = content.get("html");
        if (!html.isTextual() || html.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'html' must be a non-empty string");
        }
    }

    private void validateDefinitionContent(JsonNode content, String errorPrefix) {
        if (!content.has("term")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'term'");
        }
        JsonNode term = content.get("term");
        if (!term.isTextual() || term.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'term' must be a non-empty string");
        }

        if (!content.has("definition")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'definition'");
        }
        JsonNode definition = content.get("definition");
        if (!definition.isTextual() || definition.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'definition' must be a non-empty string");
        }
    }

    private void validateComparisonContent(JsonNode content, String errorPrefix) {
        if (!content.has("items")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Missing required field 'items'");
        }

        JsonNode items = content.get("items");
        if (!items.isArray() || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorPrefix + "Field 'items' must be a non-empty array");
        }

        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            if (!item.has("label") || !item.get("label").isTextual() || item.get("label").asText().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        errorPrefix + "Item at index " + i + " must have a non-empty 'label' field"
                );
            }
            if (!item.has("description") || !item.get("description").isTextual() || item.get("description").asText().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        errorPrefix + "Item at index " + i + " must have a non-empty 'description' field"
                );
            }
        }
    }
}
