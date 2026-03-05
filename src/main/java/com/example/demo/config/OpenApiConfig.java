package com.example.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    security = @SecurityRequirement(name = "bearerAuth"),
    info = @Info(
        title = "Alphalearn API",
        version = "v1",
        description = "API for learning content and role-based moderation workflows.",
        contact = @Contact(
            name = "Alphalearn Team",
            email = "team@alphalearn.com"
        )
    ),
    tags = {
        @Tag(name = "Lessons", description = "Public and contributor lesson endpoints"),
        @Tag(name = "Concepts (Public)", description = "Public concept read endpoints"),
        @Tag(name = "Contributors (Public)", description = "Public contributor read endpoints"),
        @Tag(name = "Learners (Public)", description = "Public learner read endpoints"),
        @Tag(name = "Lesson Enrollments", description = "Enrollment progress endpoints"),
        @Tag(name = "Me", description = "Current-user role resolution endpoint"),
        @Tag(name = "Admin Lessons", description = "Lesson moderation endpoints"),
        @Tag(name = "Admin Concepts", description = "Concept management endpoints"),
        @Tag(name = "Admin Contributors", description = "Contributor role management endpoints"),
        @Tag(name = "Admin Contributor Applications", description = "Contributor application moderation endpoints"),
        @Tag(name = "Admin Learners", description = "Learner listing endpoints for admins")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {}
