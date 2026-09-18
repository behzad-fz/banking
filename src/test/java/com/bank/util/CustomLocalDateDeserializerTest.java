package com.bank.util;

import com.bank.integration.typesafe.ChoiceAnswer;
import com.bank.integration.typesafe.ChoiceQuestion;
import com.bank.integration.typesafe.TypeSafeClient;
import com.bank.service.DateInterpretationService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomLocalDateDeserializerTest {

    static class Holder {
        public LocalDate date;
    }

    static class StubTypeSafeClient implements TypeSafeClient {

        boolean enabled = true;
        Map<String, ChoiceAnswer> answers = Map.of();

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public Map<String, ChoiceAnswer> askChoice(Object state, Map<String, ChoiceQuestion> questions) {
            return answers;
        }
    }

    private final StubTypeSafeClient client = new StubTypeSafeClient();

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new SimpleModule()
            .addDeserializer(LocalDate.class,
                    new CustomLocalDateDeserializer(new DateInterpretationService(client))));

    private static ChoiceAnswer answer(String choice, double confidence) {
        return new ChoiceAnswer(choice, Map.of(choice, confidence), confidence);
    }

    @Test
    void parsesLongForm() throws Exception {
        Holder holder = mapper.readValue("{\"date\":\"August 16 1990\"}", Holder.class);
        assertEquals(LocalDate.of(1990, 8, 16), holder.date);
    }

    @Test
    void parsesIsoDate() throws Exception {
        Holder holder = mapper.readValue("{\"date\":\"1990-08-16\"}", Holder.class);
        assertEquals(LocalDate.of(1990, 8, 16), holder.date);
    }

    @Test
    void parsesSlashFormats() throws Exception {
        Holder dmy = mapper.readValue("{\"date\":\"16/08/1990\"}", Holder.class);
        assertEquals(LocalDate.of(1990, 8, 16), dmy.date);

        Holder mdy = mapper.readValue("{\"date\":\"08/16/1990\"}", Holder.class);
        assertEquals(LocalDate.of(1990, 8, 16), mdy.date);
    }

    @Test
    void fallsBackToTypeSafeForNaturalLanguageDates() throws Exception {
        Map<String, ChoiceAnswer> answers = new LinkedHashMap<>();
        answers.put("mode", answer("absolute", 0.99));
        answers.put("month", answer("August", 0.95));
        answers.put("day", answer("16", 0.95));
        answers.put("year", answer("1990", 0.95));
        client.answers = answers;

        Holder holder = mapper.readValue("{\"date\":\"16th August, 1990\"}", Holder.class);

        assertEquals(LocalDate.of(1990, 8, 16), holder.date);
    }

    @Test
    void malformedDateReportsClearError() {
        client.enabled = false;

        JsonMappingException ex = assertThrows(JsonMappingException.class,
                () -> mapper.readValue("{\"date\":\"not-a-date\"}", Holder.class));
        assertTrue(ex.getMessage().contains("Accepted formats"),
                "error should list accepted formats, was: " + ex.getMessage());
    }

    @Test
    void missingDateIsRejected() {
        assertThrows(JsonMappingException.class,
                () -> mapper.readValue("{\"date\":\"\"}", Holder.class));
    }
}
