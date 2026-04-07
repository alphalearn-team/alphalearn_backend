package com.example.demo.lesson.authoring;

import com.example.demo.lesson.*;

import com.example.demo.config.SupabaseAuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LessonServiceValidationSupport {

    void requireContributorUser(SupabaseAuthUser user) {
        if (user == null || !user.isContributor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contributor access required");
        }
    }

    void requireOwner(Lesson lesson, SupabaseAuthUser user) {
        var ownerId = lesson.getContributor() == null ? null : lesson.getContributor().getContributorId();
        if (ownerId == null || !ownerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson owner access required");
        }
    }

    String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    boolean isEmptyContent(Object content) {
        if (content == null) {
            return true;
        }
        if (content instanceof java.util.Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }
}
