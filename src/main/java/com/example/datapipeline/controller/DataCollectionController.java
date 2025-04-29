package com.example.datapipeline.controller;

import com.example.datapipeline.scheduler.DataCollectionJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DataCollectionController {

    private final DataCollectionJob dataCollectionJob;

    @PostMapping("/collect")
    public ResponseEntity<String> triggerDataCollection() {
        try {
            dataCollectionJob.collectWeatherData();
            return ResponseEntity.ok("Data collection triggered successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error triggering data collection: " + e.getMessage());
        }
    }
}
