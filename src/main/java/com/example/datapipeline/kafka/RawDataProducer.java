// RawDataProducer.java
package com.example.datapipeline.kafka;

import com.example.datapipeline.model.RawWeatherData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RawDataProducer {
    @Value("${kafka.topic.raw-data}")
    private String topic;

    private final KafkaTemplate<String, RawWeatherData> kafkaTemplate;

    public void sendRawData(RawWeatherData rawWeatherData) {
        kafkaTemplate.send(topic, rawWeatherData.getCity(), rawWeatherData);
    }
}
