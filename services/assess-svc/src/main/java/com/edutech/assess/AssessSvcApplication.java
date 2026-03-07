// src/main/java/com/edutech/assess/AssessSvcApplication.java
package com.edutech.assess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AssessSvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssessSvcApplication.class, args);
    }
}
