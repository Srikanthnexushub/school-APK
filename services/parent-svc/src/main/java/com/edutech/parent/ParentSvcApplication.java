// src/main/java/com/edutech/parent/ParentSvcApplication.java
package com.edutech.parent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ParentSvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParentSvcApplication.class, args);
    }
}
