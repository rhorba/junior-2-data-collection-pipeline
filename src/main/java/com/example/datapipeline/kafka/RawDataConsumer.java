package com.example.datapipeline.kafka;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.model.RawWeatherData;
import com.example.datapipeline.service.DataProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawDataConsumer {
    private final DataProcessingService dataProcessingService;
    private final ProcessedDataProducer processedDataProducer;

    @KafkaListener(topics = "${kafka.topic.raw-data}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(RawWeatherData rawWeatherData) {
        log.info("Received raw weather data: {}", rawWeatherData);
        ProcessedWeatherData processedData = dataProcessingService.processRawData(rawWeatherData);
        processedDataProducer.sendProcessedData(processedData);
    }
}
