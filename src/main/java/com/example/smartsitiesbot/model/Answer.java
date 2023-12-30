package com.example.smartsitiesbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;
    private String answer;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "quizResponse_id")
    private QuizResponse quizResponse;

    public int calculateScore() {
        try {
            return Integer.parseInt(this.answer);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
