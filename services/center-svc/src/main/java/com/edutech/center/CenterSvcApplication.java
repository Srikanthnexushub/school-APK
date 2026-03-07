// src/main/java/com/edutech/center/CenterSvcApplication.java
package com.edutech.center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CenterSvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(CenterSvcApplication.class, args);
    }
}
