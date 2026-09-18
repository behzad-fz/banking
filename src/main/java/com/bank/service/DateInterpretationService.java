package com.bank.service;

import com.bank.exception.DateParseException;
import com.bank.integration.typesafe.ChoiceAnswer;
import com.bank.integration.typesafe.ChoiceQuestion;
import com.bank.integration.typesafe.TypeSafeClient;
import com.bank.integration.typesafe.TypeSafeUnavailableException;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DateInterpretationService {

    public static final String ACCEPTED_FORMATS =
            "MMMM dd yyyy, yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy";

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("MMMM dd yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    private static final String NONE = "none";
    private static final String OUT_OF_RANGE = "out_of_range";
    private static final int MIN_YEAR = 1900;
    private static final double MIN_CONFIDENCE = 0.8;

    private final TypeSafeClient typeSafeClient;

    public DateInterpretationService(TypeSafeClient typeSafeClient) {
        this.typeSafeClient = typeSafeClient;
    }

    public LocalDate parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DateParseException("Date value is missing or empty");
        }

        String trimmed = raw.trim();
        LocalDate deterministic = parseDeterministic(trimmed);
        if (deterministic != null) {
            return deterministic;
        }

        return parseWithTypeSafe(trimmed);
    }

    private LocalDate parseDeterministic(String value) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next accepted format
            }
        }
        return null;
    }

    private LocalDate parseWithTypeSafe(String value) {
        if (!typeSafeClient.isEnabled()) {
            throw unparsed(value);
        }

        Map<String, ChoiceAnswer> answers;
        try {
            answers = typeSafeClient.askChoice(value, dateQuestions());
        } catch (TypeSafeUnavailableException e) {
            throw unparsed(value);
        }

        return assemble(value, answers);
    }

    private LocalDate assemble(String value, Map<String, ChoiceAnswer> answers) {
        ChoiceAnswer mode = answers.get("mode");
        ChoiceAnswer month = answers.get("month");
        ChoiceAnswer day = answers.get("day");
        ChoiceAnswer year = answers.get("year");

        if (mode == null || !"absolute".equals(mode.choice())) {
            throw unparsed(value);
        }
        if (!isUsable(month) || !isUsable(day) || !isUsable(year)) {
            throw unparsed(value);
        }

        double confidence = Math.min(
                Math.min(mode.confidence(), month.confidence()),
                Math.min(day.confidence(), year.confidence()));
        if (confidence < MIN_CONFIDENCE) {
            throw unparsed(value);
        }

        try {
            int monthValue = Month.valueOf(month.choice().toUpperCase(Locale.ROOT)).getValue();
            int dayValue = Integer.parseInt(day.choice());
            int yearValue = Integer.parseInt(year.choice());

            if (yearValue < MIN_YEAR || yearValue > LocalDate.now().getYear()) {
                throw unparsed(value);
            }

            return LocalDate.of(yearValue, monthValue, dayValue);
        } catch (NumberFormatException | DateTimeException e) {
            throw unparsed(value);
        }
    }

    private boolean isUsable(ChoiceAnswer answer) {
        return answer != null
                && answer.choice() != null
                && !NONE.equals(answer.choice())
                && !OUT_OF_RANGE.equals(answer.choice());
    }

    private Map<String, ChoiceQuestion> dateQuestions() {
        Map<String, ChoiceQuestion> questions = new LinkedHashMap<>();
        questions.put("mode", new ChoiceQuestion(
                "How is the date written? 'absolute' = a calendar date naming a month "
                        + "(e.g. '16 August 1990', 'August 16, 1990'); "
                        + "'relative' = relative to today (e.g. 'yesterday', 'next Friday'); "
                        + "'none' = the text does not state a calendar date.",
                criteria("absolute", "relative", NONE)));

        Map<String, String> monthCriteria = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            monthCriteria.put(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH), null);
        }
        monthCriteria.put(NONE, "The date does not state a month.");
        questions.put("month", new ChoiceQuestion(
                "If the date is an absolute calendar date, which month is it in?",
                monthCriteria));

        Map<String, String> dayCriteria = new LinkedHashMap<>();
        for (int day = 1; day <= 31; day++) {
            dayCriteria.put(String.valueOf(day), null);
        }
        dayCriteria.put(NONE, "The date does not state a day of the month.");
        questions.put("day", new ChoiceQuestion(
                "If the date is an absolute calendar date, which day of the month (1-31) is it?",
                dayCriteria));

        Map<String, String> yearCriteria = new LinkedHashMap<>();
        for (int year = MIN_YEAR; year <= LocalDate.now().getYear(); year++) {
            yearCriteria.put(String.valueOf(year), null);
        }
        yearCriteria.put(OUT_OF_RANGE, "A year is stated but is outside the listed range.");
        yearCriteria.put(NONE, "The date does not state a year.");
        questions.put("year", new ChoiceQuestion(
                "If the date is an absolute calendar date, which year is it?",
                yearCriteria));

        return questions;
    }

    private Map<String, String> criteria(String... options) {
        Map<String, String> criteria = new LinkedHashMap<>();
        for (String option : options) {
            criteria.put(option, null);
        }
        return criteria;
    }

    private DateParseException unparsed(String value) {
        return new DateParseException("Failed to parse date: '" + value
                + "'. Accepted formats: " + ACCEPTED_FORMATS);
    }
}
