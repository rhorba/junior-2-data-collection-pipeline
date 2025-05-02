package com.example.datapipeline.service;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.model.RawWeatherData;
import com.example.datapipeline.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataProcessingService {
    private final WeatherDataRepository weatherDataRepository;
    private final KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate;

    public ProcessedWeatherData processRawData(RawWeatherData rawData) {
        try {
            log.info("Processing raw weather data for city: {}", rawData.getCity());

            // Extract weather data from the raw data
            Map<String, Object> weatherData = rawData.getWeatherData();

            // Extract and convert main data
            Map<String, Object> mainData = (Map<String, Object>) weatherData.get("main");
            double temperature = getDoubleValue(mainData, "temp");
            double humidity = getDoubleValue(mainData, "humidity");

            // Extract and convert wind data
            Map<String, Object> windData = (Map<String, Object>) weatherData.get("wind");
            double windSpeed = getDoubleValue(windData, "speed");

            // Extract weather description
            String description = extractWeatherDescription(weatherData);

            // Create processed data object
            ProcessedWeatherData processedData = new ProcessedWeatherData(
                    rawData.getCity(),
                    rawData.getCountry(),
                    temperature,
                    humidity,
                    windSpeed,
                    description);

            // Save to database
            ProcessedWeatherData savedData = weatherDataRepository.save(processedData);

            // Publish to Kafka
            kafkaTemplate.send("processed-data", savedData);

            log.info("Weather data processed and saved: {}", savedData);

            return savedData;
        } catch (Exception e) {
            log.error("Error processing weather data: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing weather data", e);
        }
    }

    /**
     * Extract weather description from weather data
     */
    private String extractWeatherDescription(Map<String, Object> weatherData) {
        String description = "";
        try {
            if (weatherData.containsKey("weather")) {
                List<Map<String, Object>> weather = (List<Map<String, Object>>) weatherData.get("weather");
                if (weather != null && !weather.isEmpty()) {
                    description = (String) weather.get(0).get("description");
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract weather description: {}", e.getMessage());
        }
        return description != null ? description : "";
    }

    /**
     * Safely extract and convert a double value from a map
     */
    private double getDoubleValue(Map<String, Object> data, String key) {
        try {
            Object value = data.get(key);
            if (value == null) {
                return 0.0;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            } else {
                return Double.parseDouble(value.toString());
            }
        } catch (Exception e) {
            log.warn("Could not convert value for key '{}': {}", key, e.getMessage());
            return 0.0;
        }
    }
}
