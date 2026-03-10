package com.example.demo.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Types of lesson sections")
public enum SectionType {
    @Schema(description = "Rich text content section")
    TEXT,
    
    @Schema(description = "Example usage section")
    EXAMPLE,
    
    @Schema(description = "Callout/alert section")
    CALLOUT,
    
    @Schema(description = "Term definition section")
    DEFINITION,
    
    @Schema(description = "Comparison section")
    COMPARISON
}
