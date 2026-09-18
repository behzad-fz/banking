package com.bank.integration.typesafe;

import java.util.Map;

public interface TypeSafeClient {

    boolean isEnabled();

    Map<String, ChoiceAnswer> askChoice(Object state, Map<String, ChoiceQuestion> questions);
}
