package com.example.demo.quiz;

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
@Table(name = "true_false_questions")
@DiscriminatorValue("true-false")
public class TrueFalseQuestion extends QuizQuestion {

    @Column(name = "correct_boolean", nullable = false)
    private boolean correctBoolean;

    public TrueFalseQuestion(Quiz quiz, String prompt, int orderIndex, boolean correctBoolean) {
        super(quiz, prompt, orderIndex);
        this.correctBoolean = correctBoolean;
    }
}
