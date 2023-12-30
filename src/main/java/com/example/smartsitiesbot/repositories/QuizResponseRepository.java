package com.example.smartsitiesbot.repositories;

import com.example.smartsitiesbot.model.QuizResponse;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuizResponseRepository extends CrudRepository<QuizResponse, Long> {
    QuizResponse findByCity (String city);
    List<QuizResponse> findAllByCity (String city);
}
