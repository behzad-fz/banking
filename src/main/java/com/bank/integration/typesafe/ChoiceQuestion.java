package com.bank.integration.typesafe;

import java.util.Map;

public record ChoiceQuestion(String instructions, Map<String, String> criteria) {
}
