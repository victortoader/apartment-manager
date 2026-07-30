package com.apartmentmanager.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DefaultPassword {

    private static final String VAR_NAME = "DEFAULT_PASSWORD";

    private DefaultPassword() {
    }

    public static String require() {
        String password = System.getenv(VAR_NAME);
        if (password != null && !password.isBlank()) {
            return password;
        }
        String fromEnvFile = fromDotEnv();
        if (fromEnvFile != null && !fromEnvFile.isBlank()) {
            return fromEnvFile;
        }
        throw new IllegalStateException("DEFAULT_PASSWORD environment variable is required to seed default users");
    }

    private static String fromDotEnv() {
        try {
            Path envFile = Path.of(".env");
            if (!Files.exists(envFile)) {
                return null;
            }
            return Files.lines(envFile)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> line.startsWith(VAR_NAME + "="))
                    .map(line -> line.substring(VAR_NAME.length() + 1))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
