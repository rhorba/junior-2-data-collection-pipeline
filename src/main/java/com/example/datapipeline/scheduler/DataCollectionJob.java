package com.example.datapipeline.scheduler;

import com.example.datapipeline.client.WeatherApiClient;
import com.example.datapipeline.kafka.RawDataProducer;
import com.example.datapipeline.model.RawWeatherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCollectionJob {

    private final WeatherApiClient weatherApiClient;
    private final RawDataProducer rawDataProducer;

    @Value("${api.weather.cities}")
    private String citiesConfig;

    @Value("${app.scheduling.enable}")
    private boolean schedulingEnabled;

    @Scheduled(fixedRateString = "${app.scheduling.frequency}")
    public void collectWeatherData() {
        if (!schedulingEnabled) {
            return;
        }

        log.info("Starting scheduled weather data collection");
        List<String> cities = Arrays.asList(citiesConfig.split(","));

        for (String city : cities) {
            try {
                RawWeatherData rawData = weatherApiClient.getWeatherData(city.trim());
                if (rawData != null) {
                    log.info("Collected weather data for city: {}", city);
                    rawDataProducer.sendRawData(rawData);
                }
            } catch (Exception e) {
                log.error("Error collecting weather data for city {}: {}", city, e.getMessage(), e);
            }
        }

        log.info("Completed scheduled weather data collection");
    }
}
