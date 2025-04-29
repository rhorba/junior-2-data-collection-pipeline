package com.example.datapipeline.kafka;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedDataConsumer {
    private final WeatherDataRepository weatherDataRepository;

    @KafkaListener(topics = "${kafka.topic.processed-data}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ProcessedWeatherData processedWeatherData) {
        log.info("Received processed weather data: {}", processedWeatherData);
        weatherDataRepository.save(processedWeatherData);
    }
}
