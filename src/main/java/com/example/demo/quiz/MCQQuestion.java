package com.example.demo.quiz;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mcq_questions")
@DiscriminatorValue("multiple-choice")
public class MCQQuestion extends QuizQuestion {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb", nullable = false)
    private JsonNode options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_option_ids", columnDefinition = "jsonb", nullable = false)
    private JsonNode correctOptionIds;

    public MCQQuestion(Quiz quiz, String prompt, int orderIndex, JsonNode options, JsonNode correctOptionIds) {
        super(quiz, prompt, orderIndex);
        this.options = options;
        this.correctOptionIds = correctOptionIds;
    }
}
