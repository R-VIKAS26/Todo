package com.vikasr.todo.config;

import java.util.Arrays;

final class OriginValueParser {

    private OriginValueParser() {
    }

    static String[] parse(String configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(configuredOrigins.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }
}
