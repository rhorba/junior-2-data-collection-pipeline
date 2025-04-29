package com.example.datapipeline.service;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.model.RawWeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataProcessingService {

    public ProcessedWeatherData processRawData(RawWeatherData rawData) {
        Map<String, Object> weatherData = rawData.getWeatherData();
        try {
            // Extract main weather data
            Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
            Double temperature = convertKelvinToCelsius((Double) main.get("temp"));
            Double humidity = (Double) main.get("humidity");

            // Extract wind data
            Map<String, Object> wind = (Map<String, Object>) weatherData.get("wind");
            Double windSpeed = (Double) wind.get("speed");

            // Extract weather description
            List<Map<String, Object>> weather =
                    (List<Map<String, Object>>) weatherData.get("weather");
            String description = (String) weather.get(0).get("description");

            // Create processed data object
            return new ProcessedWeatherData(
                    rawData.getCity(),
                    rawData.getCountry(),
                    temperature,
                    humidity,
                    windSpeed,
                    description
            );

        } catch (Exception e) {
            log.error("Error processing weather data: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing weather data", e);
        }
    }

    private Double convertKelvinToCelsius(Double kelvin) {
        return kelvin - 273.15;
    }
}
