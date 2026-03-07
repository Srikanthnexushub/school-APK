package com.edutech.student;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudentProfileSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentProfileSvcApplication.class, args);
    }
}
