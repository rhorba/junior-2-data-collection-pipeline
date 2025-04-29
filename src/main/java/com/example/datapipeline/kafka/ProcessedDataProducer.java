package com.example.datapipeline.kafka;

import com.example.datapipeline.model.ProcessedWeatherData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessedDataProducer {
    @Value("${kafka.topic.processed-data}")
    private String topic;

    private final KafkaTemplate<String, ProcessedWeatherData> kafkaTemplate;

    public void sendProcessedData(ProcessedWeatherData processedWeatherData) {
        kafkaTemplate.send(topic, processedWeatherData.getCity(), processedWeatherData);
    }
}
