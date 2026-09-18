package com.bank.integration.typesafe;

public class TypeSafeUnavailableException extends RuntimeException {

    public TypeSafeUnavailableException(String message) {
        super(message);
    }

    public TypeSafeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
