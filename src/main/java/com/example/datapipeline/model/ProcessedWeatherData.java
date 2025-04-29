package com.example.datapipeline.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;
    private String country;
    private Double temperature; // in Celsius
    private Double humidity; // percentage
    private Double windSpeed; // in m/s
    private String weatherDescription;
    private LocalDateTime timestamp;

    public ProcessedWeatherData(String city, String country, Double temperature,
                                Double humidity, Double windSpeed, String weatherDescription) {
        this.city = city;
        this.country = country;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherDescription = weatherDescription;
        this.timestamp = LocalDateTime.now();
    }
}
