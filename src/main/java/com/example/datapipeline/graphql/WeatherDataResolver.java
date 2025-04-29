package com.example.datapipeline.graphql;

import com.example.datapipeline.model.ProcessedWeatherData;
import com.example.datapipeline.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WeatherDataResolver {

    private final WeatherDataRepository weatherDataRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @QueryMapping
    public List<ProcessedWeatherData> allWeatherData() {
        return weatherDataRepository.findAll();
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCity(@Argument String city) {
        return weatherDataRepository.findByCity(city);
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCountry(@Argument String country) {
        return weatherDataRepository.findByCountry(country);
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByDateRange(@Argument DateRangeInput dateRange) {
        LocalDateTime start = LocalDateTime.parse(dateRange.getStartDate(), DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(dateRange.getEndDate(), DATE_FORMATTER);
        return weatherDataRepository.findByTimestampBetween(start, end);
    }

    @QueryMapping
    public List<ProcessedWeatherData> weatherDataByCityAndDateRange(
            @Argument String city,
            @Argument DateRangeInput dateRange) {
        LocalDateTime start = LocalDateTime.parse(dateRange.getStartDate(), DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(dateRange.getEndDate(), DATE_FORMATTER);
        return weatherDataRepository.findByCityAndTimestampBetween(city, start, end);
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
