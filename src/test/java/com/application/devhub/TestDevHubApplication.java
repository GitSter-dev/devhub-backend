package com.application.devhub;

import org.springframework.boot.SpringApplication;

public class TestDevHubApplication {

    public static void main(String[] args) {
        SpringApplication.from(DevHubApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
