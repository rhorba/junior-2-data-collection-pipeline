package com.example.datapipeline.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawWeatherData {
    private String city;
    private String country;
    private Map<String, Object> weatherData;
    private LocalDateTime timestamp;

    public RawWeatherData(String city, String country, Map<String, Object> weatherData) {
        this.city = city;
        this.country = country;
        this.weatherData = weatherData;
        this.timestamp = LocalDateTime.now();
    }
}
