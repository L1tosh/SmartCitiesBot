package com.example.smartsitiesbot.model;

import com.example.smartsitiesbot.model.enums.QuizState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "quiz_responses")
@Table
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizResponse {
    @Id
    private Long id;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "city")
    private String city;
    private int countOfQuestion;
    private QuizState quizState;
    @OneToMany(mappedBy = "quizResponse", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Answer> answerList = new ArrayList<>();

}
