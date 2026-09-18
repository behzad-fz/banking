package com.bank.util;

import com.bank.exception.DateParseException;
import com.bank.service.DateInterpretationService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;

public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private final DateInterpretationService dateInterpretationService;

    public CustomLocalDateDeserializer(DateInterpretationService dateInterpretationService) {
        this.dateInterpretationService = dateInterpretationService;
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            return dateInterpretationService.parse(p.getValueAsString());
        } catch (DateParseException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
