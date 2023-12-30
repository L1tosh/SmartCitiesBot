package com.example.smartsitiesbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityStatistics {
    private String city;
    private int totalScore;
    private int count;
    private double averageScore;
}
