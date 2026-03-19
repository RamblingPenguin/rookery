package com.ramblingpenguin.rookery.examples.plugin;

import com.google.common.base.CharMatcher;
import com.ramblingpenguin.rookery.examples.api.TextProcessor;

public class GuavaTextProcessor implements TextProcessor {
    @Override
    public String process(String input) {
        if (input == null) return null;
        // Removes all digits using Google Guava to prove transitive dependency loading
        return CharMatcher.inRange('0', '9').removeFrom(input);
    }
}
