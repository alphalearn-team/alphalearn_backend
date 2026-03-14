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
@DiscriminatorValue("single-choice")
public class MCQQuestion extends QuizQuestion {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb", nullable = false)
    private JsonNode options;

    @Column(name = "correct_option_id", nullable = false)
    private String correctOptionId;

    public MCQQuestion(Quiz quiz, String prompt, int orderIndex, JsonNode options, String correctOptionId) {
        super(quiz, prompt, orderIndex);
        this.options = options;
        this.correctOptionId = correctOptionId;
    }
}
