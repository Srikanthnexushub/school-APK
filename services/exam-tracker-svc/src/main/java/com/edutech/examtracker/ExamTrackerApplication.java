package com.edutech.examtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExamTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamTrackerApplication.class, args);
    }
}
