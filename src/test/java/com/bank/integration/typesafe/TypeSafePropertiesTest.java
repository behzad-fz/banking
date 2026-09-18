package com.bank.integration.typesafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TypeSafePropertiesTest {

    @Test
    void toStringDoesNotExposeTheApiKey() {
        TypeSafeProperties properties = new TypeSafeProperties();
        properties.setApiKey("super-secret-key");

        assertFalse(properties.toString().contains("super-secret-key"),
                "toString must not leak the API key: " + properties);
    }
}
