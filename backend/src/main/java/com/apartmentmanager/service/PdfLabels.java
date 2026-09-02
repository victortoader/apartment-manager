package com.apartmentmanager.service;

import java.util.Map;
import java.util.Set;

public class PdfLabels {

    private static final Set<String> VALID_KEYS = Set.of(
        "datePrefix", "outTenant", "inTenant", "firstName", "name", "address", "postalCode", "city", "phone",
        "email", "property", "objectNumber", "rentalObject", "incomingParty", "detail", "text", "photos", "new",
        "normal", "defect", "missing", "costShare", "confirmations", "signatures", "cityPrefix", "page", "photoIndex"
    );

    private static final Map<String, String> DEFAULT_LABELS = Map.ofEntries(
        Map.entry("datePrefix", "vom"),
        Map.entry("outTenant", "Ausziehender Mieter"),
        Map.entry("inTenant", "Einziehender Mieter"),
        Map.entry("firstName", "Vorname"),
        Map.entry("name", "Name"),
        Map.entry("address", "Adresse"),
        Map.entry("postalCode", "PLZ"),
        Map.entry("city", "Ort"),
        Map.entry("phone", "Tel."),
        Map.entry("email", "E-Mail"),
        Map.entry("property", "Liegenschaft"),
        Map.entry("objectNumber", "Objektnr."),
        Map.entry("rentalObject", "Mietobjekt"),
        Map.entry("incomingParty", "Einzugspartei"),
        Map.entry("detail", "Detail"),
        Map.entry("text", "Text"),
        Map.entry("photos", "Fotos"),
        Map.entry("new", "Neu"),
        Map.entry("normal", "Normal"),
        Map.entry("defect", "Mangel"),
        Map.entry("missing", "Fehlend"),
        Map.entry("costShare", "Kostenanteil"),
        Map.entry("confirmations", "Bestaetigungen"),
        Map.entry("signatures", "Unterschriften"),
        Map.entry("cityPrefix", "Ort"),
        Map.entry("page", "Seite"),
        Map.entry("photoIndex", "Fotoverzeichnis")
    );

    private final Map<String, String> labels;

    public PdfLabels(Map<String, String> labels) {
        this.labels = labels == null ? Map.of() : labels;
    }

    public String get(String key) {
        String value = labels.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        String defaultValue = DEFAULT_LABELS.get(key);
        return defaultValue != null ? defaultValue : key;
    }

    public static boolean isValidLabelKey(String key) {
        return VALID_KEYS.contains(key);
    }
}
