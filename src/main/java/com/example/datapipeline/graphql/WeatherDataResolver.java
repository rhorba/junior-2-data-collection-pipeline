package com.example.datapipeline.graphql;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.repository.WeatherDataRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WeatherDataResolver {

    private final WeatherDataRepository weatherDataRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    // Ensure the constructor is properly injecting the repository
    public WeatherDataResolver(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
    }

    @QueryMapping
    public List<ProcessedWeatherData> allWeatherData() {
        // Return an empty list instead of null if no data exists
        List<ProcessedWeatherData> data = weatherDataRepository.findAll();
        return data != null ? data : new ArrayList<>();
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCity(@Argument String city) {
        // Return an empty list instead of null if no data exists
        List<ProcessedWeatherData> data = weatherDataRepository.findByCity(city);
        return data != null ? data : new ArrayList<>();
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCountry(@Argument String country) {
        // Return an empty list instead of null if no data exists
        List<ProcessedWeatherData> data = weatherDataRepository.findByCountry(country);
        return data != null ? data : new ArrayList<>();
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByDateRange(@Argument DateRangeInput dateRange) {
        LocalDateTime start = LocalDateTime.parse(dateRange.getStartDate(), DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(dateRange.getEndDate(), DATE_FORMATTER);

        // Return an empty list instead of null if no data exists
        List<ProcessedWeatherData> data = weatherDataRepository.findByTimestampBetween(start, end);
        return data != null ? data : new ArrayList<>();
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCityAndDateRange(
            @Argument String city,
            @Argument DateRangeInput dateRange) {
        LocalDateTime start = LocalDateTime.parse(dateRange.getStartDate(), DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(dateRange.getEndDate(), DATE_FORMATTER);

        // Return an empty list instead of null if no data exists
        List<ProcessedWeatherData> data = weatherDataRepository.findByCityAndTimestampBetween(city, start, end);
        return data != null ? data : new ArrayList<>();
    }

    // Input class for date range
    public static class DateRangeInput {
        private String startDate;
        private String endDate;

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }
}
