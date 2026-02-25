package com.example.demo.lesson.query;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ConceptsMatchMode {
    ANY,
    ALL;

    public static ConceptsMatchMode fromRequest(String value) {
        if (value == null) {
            return ANY;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "any" -> ANY;
            case "all" -> ALL;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "conceptsMatch must be 'any' or 'all'"
            );
        };
    }
}
