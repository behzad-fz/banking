package com.bank.integration.typesafe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "typesafe")
public class TypeSafeProperties {

    private String apiKey = "";

    private String model = "jev-latest";

    private String baseUrl = "https://api.typesafe.ai";

    private Duration timeout = Duration.ofSeconds(3);
}
