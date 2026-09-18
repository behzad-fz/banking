package com.bank.service;

import com.bank.exception.DateParseException;
import com.bank.integration.typesafe.ChoiceAnswer;
import com.bank.integration.typesafe.ChoiceQuestion;
import com.bank.integration.typesafe.TypeSafeClient;
import com.bank.integration.typesafe.TypeSafeUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateInterpretationServiceTest {

    private final FakeTypeSafeClient client = new FakeTypeSafeClient();
    private final DateInterpretationService service = new DateInterpretationService(client);

    static class FakeTypeSafeClient implements TypeSafeClient {

        boolean enabled = true;
        Map<String, ChoiceAnswer> answers = Map.of();
        RuntimeException failure;
        int calls;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public Map<String, ChoiceAnswer> askChoice(Object state, Map<String, ChoiceQuestion> questions) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return answers;
        }
    }

    private static ChoiceAnswer answer(String choice, double confidence) {
        return new ChoiceAnswer(choice, Map.of(choice, confidence), confidence);
    }

    private static Map<String, ChoiceAnswer> absoluteDate(String month, String day, String year, double confidence) {
        Map<String, ChoiceAnswer> answers = new LinkedHashMap<>();
        answers.put("mode", answer("absolute", 0.99));
        answers.put("month", answer(month, confidence));
        answers.put("day", answer(day, confidence));
        answers.put("year", answer(year, confidence));
        return answers;
    }

    @Test
    void parsesSupportedDeterministicFormatsWithoutCallingTypeSafe() {
        assertEquals(LocalDate.of(1990, 8, 16), service.parse("1990-08-16"));
        assertEquals(LocalDate.of(1990, 8, 16), service.parse("16/08/1990"));
        assertEquals(LocalDate.of(1990, 8, 16), service.parse("08/16/1990"));
        assertEquals(LocalDate.of(1990, 8, 16), service.parse("August 16 1990"));

        assertEquals(0, client.calls, "deterministic formats must not hit TypeSafe");
    }

    @Test
    void fallsBackToTypeSafeWhenNoFormatMatches() {
        client.answers = absoluteDate("August", "16", "1990", 0.95);

        assertEquals(LocalDate.of(1990, 8, 16), service.parse("16th August, 1990"));
        assertEquals(1, client.calls);
    }

    @Test
    void rejectsRelativeDateFromTypeSafe() {
        Map<String, ChoiceAnswer> answers = new LinkedHashMap<>(absoluteDate("August", "16", "1990", 0.95));
        answers.put("mode", answer("relative", 0.95));
        client.answers = answers;

        assertThrows(DateParseException.class, () -> service.parse("sixteen days ago"));
    }

    @Test
    void rejectsMissingPart() {
        client.answers = absoluteDate("none", "16", "1990", 0.95);

        assertThrows(DateParseException.class, () -> service.parse("the 16th of some month"));
    }

    @Test
    void rejectsYearOutsideTheOfferedRange() {
        client.answers = absoluteDate("August", "16", "out_of_range", 0.95);

        assertThrows(DateParseException.class, () -> service.parse("16 August 1780"));
    }

    @Test
    void rejectsImpossibleCalendarDate() {
        client.answers = absoluteDate("February", "30", "1990", 0.95);

        assertThrows(DateParseException.class, () -> service.parse("30 February 1990"));
    }

    @Test
    void rejectsLowConfidenceAnswers() {
        client.answers = absoluteDate("August", "16", "1990", 0.5);

        assertThrows(DateParseException.class, () -> service.parse("maybe August 1990"));
    }

    @Test
    void rejectsIncompleteAnswers() {
        Map<String, ChoiceAnswer> answers = new LinkedHashMap<>(absoluteDate("August", "16", "1990", 0.95));
        answers.remove("year");
        client.answers = answers;

        assertThrows(DateParseException.class, () -> service.parse("16th August"));
    }

    @Test
    void reportsAcceptedFormatsWhenTypeSafeIsDisabled() {
        client.enabled = false;

        DateParseException ex = assertThrows(DateParseException.class,
                () -> service.parse("16th August, 1990"));

        assertTrue(ex.getMessage().contains("Accepted formats"), "was: " + ex.getMessage());
    }

    @Test
    void reportsAcceptedFormatsWhenTypeSafeFails() {
        client.failure = new TypeSafeUnavailableException("timed out");

        DateParseException ex = assertThrows(DateParseException.class,
                () -> service.parse("16th August, 1990"));

        assertTrue(ex.getMessage().contains("Accepted formats"), "was: " + ex.getMessage());
    }

    @Test
    void rejectsMissingOrBlankInput() {
        DateParseException missing = assertThrows(DateParseException.class, () -> service.parse(null));
        assertTrue(missing.getMessage().contains("missing or empty"));

        DateParseException blank = assertThrows(DateParseException.class, () -> service.parse("   "));
        assertTrue(blank.getMessage().contains("missing or empty"));
    }
}
