package com.bank.integration.typesafe;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HttpTypeSafeClient implements TypeSafeClient {

    private static final String EVALUATION_PATH = "/v1/systemone";
    private static final String DEFAULT_BASE_URL = "https://api.typesafe.ai";

    private final WebClient webClient;
    private final TypeSafeProperties properties;

    public HttpTypeSafeClient(WebClient webClient, TypeSafeProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public Map<String, ChoiceAnswer> askChoice(Object state, Map<String, ChoiceQuestion> questions) {
        if (!isEnabled()) {
            throw new TypeSafeUnavailableException("TypeSafe is not configured (typesafe.api-key is blank)");
        }

        try {
            JsonNode response = webClient.post()
                    .uri(baseUrl() + EVALUATION_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(buildRequestBody(state, questions))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(properties.getTimeout());

            if (response == null) {
                throw new TypeSafeUnavailableException("TypeSafe returned an empty response");
            }

            return parseAnswers(response);
        } catch (TypeSafeUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new TypeSafeUnavailableException("TypeSafe request failed: " + e.getMessage(), e);
        }
    }

    Map<String, Object> buildRequestBody(Object state, Map<String, ChoiceQuestion> questions) {
        Map<String, Object> questionBodies = new LinkedHashMap<>();
        questions.forEach((id, question) -> {
            Map<String, Object> questionBody = new LinkedHashMap<>();
            questionBody.put("type", "choice");
            questionBody.put("instructions", question.instructions());
            questionBody.put("criteria", question.criteria());
            questionBodies.put(id, questionBody);
        });

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("state", state);
        requestBody.put("questions", questionBodies);
        return requestBody;
    }

    Map<String, ChoiceAnswer> parseAnswers(JsonNode root) {
        Map<String, ChoiceAnswer> answers = new LinkedHashMap<>();
        JsonNode answersNode = root.path("answers");

        answersNode.fields().forEachRemaining(entry -> {
            JsonNode answer = entry.getValue();
            if (!"choice".equals(answer.path("type").asText())) {
                return;
            }

            Map<String, Double> probabilities = new LinkedHashMap<>();
            answer.path("probabilities").fields()
                    .forEachRemaining(p -> probabilities.put(p.getKey(), p.getValue().asDouble()));

            answers.put(entry.getKey(), new ChoiceAnswer(
                    answer.path("choice").asText(),
                    probabilities,
                    answer.path("confidence").asDouble()));
        });

        return answers;
    }

    private String baseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
