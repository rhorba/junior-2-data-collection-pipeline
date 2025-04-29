package com.example.datapipeline.client;

import com.example.datapipeline.model.RawWeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class WeatherApiClient {

    private final WebClient webClient;

    @Value("${api.weather.key}")
    private String apiKey;

    public WeatherApiClient(@Value("${api.weather.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public RawWeatherData getWeatherData(String city) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                Map<String, Object> sys = (Map<String, Object>) response.get("sys");
                String country = (String) sys.get("country");
                return new RawWeatherData(city, country, response);
            } else {
                log.error("Failed to fetch weather data for city: {}", city);
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching weather data for city {}: {}", city, e.getMessage(), e);
            return null;
        }
    }
}
