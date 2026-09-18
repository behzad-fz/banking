package com.bank.integration.typesafe;

import java.util.Map;

public record ChoiceAnswer(String choice, Map<String, Double> probabilities, double confidence) {
}
