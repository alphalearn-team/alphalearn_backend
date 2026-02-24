package com.example.demo.concept.dto;

import java.time.OffsetDateTime;


public record ConceptDTO (

    Integer conceptId,
    String title,
    String description,
    OffsetDateTime createdAt
    
) {}
