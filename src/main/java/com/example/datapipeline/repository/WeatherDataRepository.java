package com.example.datapipeline.repository;

import com.example.datapipeline.model.ProcessedWeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeatherDataRepository extends JpaRepository<ProcessedWeatherData, Long> {
    List<ProcessedWeatherData> findByCity(String city);
    List<ProcessedWeatherData> findByCountry(String country);
    List<ProcessedWeatherData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<ProcessedWeatherData> findByCityAndTimestampBetween(String city, LocalDateTime start, LocalDateTime end);
}
