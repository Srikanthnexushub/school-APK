package com.edutech.mentorsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class MentorSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorSvcApplication.class, args);
    }
}
