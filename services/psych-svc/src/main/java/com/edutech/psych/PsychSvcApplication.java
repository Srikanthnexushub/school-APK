package com.edutech.psych;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PsychSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsychSvcApplication.class, args);
    }
}
