package com.bank.config;

import com.bank.service.DateInterpretationService;
import com.bank.util.CustomLocalDateDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class JacksonConfig {

    @Bean
    SimpleModule customDateDeserializerModule(DateInterpretationService dateInterpretationService) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer(dateInterpretationService));
        return module;
    }
}
