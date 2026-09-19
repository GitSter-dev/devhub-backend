package com.application.devhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DevHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevHubApplication.class, args);
    }

}
