package com.bank.integration.typesafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTypeSafeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpTypeSafeClient client(TypeSafeProperties properties) {
        return new HttpTypeSafeClient(WebClient.builder().build(), properties);
    }

    private TypeSafeProperties propertiesWithKey(String apiKey) {
        TypeSafeProperties properties = new TypeSafeProperties();
        properties.setApiKey(apiKey);
        return properties;
    }

    @Test
    void disabledWhenApiKeyIsBlank() {
        HttpTypeSafeClient client = client(propertiesWithKey("   "));

        assertFalse(client.isEnabled());
        assertThrows(TypeSafeUnavailableException.class,
                () -> client.askChoice("16 August 1990", Map.of()));
    }

    @Test
    void enabledWhenApiKeyIsPresent() {
        assertTrue(client(propertiesWithKey("test-key")).isEnabled());
    }

    @Test
    void buildsChoiceRequestWithModelStateAndTypedQuestions() throws Exception {
        TypeSafeProperties properties = propertiesWithKey("test-key");
        properties.setModel("jev-latest");

        Map<String, String> monthCriteria = new LinkedHashMap<>();
        monthCriteria.put("August", null);
        monthCriteria.put("none", "not stated");
        Map<String, ChoiceQuestion> questions = Map.of(
                "month", new ChoiceQuestion("Which month is the date in?", monthCriteria));

        Map<String, Object> body = client(properties).buildRequestBody("16 August 1990", questions);

        JsonNode root = objectMapper.valueToTree(body);
        assertEquals("jev-latest", root.get("model").asText());
        assertEquals("16 August 1990", root.get("state").asText());
        JsonNode month = root.get("questions").get("month");
        assertEquals("choice", month.get("type").asText());
        assertEquals("Which month is the date in?", month.get("instructions").asText());
        assertTrue(month.get("criteria").has("August"));
        assertTrue(month.get("criteria").get("August").isNull());
        assertEquals("not stated", month.get("criteria").get("none").asText());
    }

    @Test
    void parsesChoiceAnswersFromApiResponse() throws Exception {
        String response = """
                {
                  "model": "jev-latest",
                  "answers": {
                    "month": {
                      "type": "choice",
                      "choice": "August",
                      "probabilities": {"August": 0.97, "July": 0.03},
                      "confidence": 0.94
                    }
                  },
                  "usage": {"input_tokens": 312, "output_tokens": 48}
                }
                """;
        JsonNode root = objectMapper.readTree(response);

        Map<String, ChoiceAnswer> answers = client(propertiesWithKey("test-key")).parseAnswers(root);

        ChoiceAnswer month = answers.get("month");
        assertEquals("August", month.choice());
        assertEquals(0.97, month.probabilities().get("August"), 1e-9);
        assertEquals(0.94, month.confidence(), 1e-9);
    }
}
