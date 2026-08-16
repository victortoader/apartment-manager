package com.apartmentmanager.service;

import java.util.Map;

public class PdfLabels {

    private final Map<String, String> labels;

    public PdfLabels(Map<String, String> labels) {
        this.labels = labels == null ? Map.of() : labels;
    }

    public String get(String key) {
        String value = labels.get(key);
        return (value == null || value.isBlank()) ? key : value;
    }
}
