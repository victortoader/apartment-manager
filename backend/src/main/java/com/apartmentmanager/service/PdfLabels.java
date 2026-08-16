package com.apartmentmanager.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PdfLabels {

    private static final Map<String, Map<String, String>> LABELS = build();

    private final Map<String, String> labels;

    public PdfLabels(String lang) {
        String code = lang == null ? "de" : lang.toLowerCase(Locale.ROOT);
        if (code.contains("-")) code = code.substring(0, code.indexOf('-'));
        this.labels = LABELS.getOrDefault(code, LABELS.get("de"));
    }

    public String get(String key) {
        return labels.getOrDefault(key, LABELS.get("de").getOrDefault(key, key));
    }

    private static Map<String, Map<String, String>> build() {
        Map<String, Map<String, String>> all = new HashMap<>();
        all.put("de", l(
            "datePrefix", "vom",
            "outTenant", "Ausziehender Mieter",
            "inTenant", "Einziehender Mieter",
            "firstName", "Vorname",
            "name", "Name",
            "address", "Adresse",
            "postalCode", "PLZ",
            "city", "Ort",
            "phone", "Telefon",
            "email", "E-Mail",
            "property", "Liegenschaft",
            "objectNumber", "Objekt-Nr.",
            "rentalObject", "Mietobjekt",
            "incomingParty", "Einziehende Partei",
            "detail", "Detail",
            "text", "Text",
            "photos", "Bilder",
            "new", "Neu",
            "normal", "Normal",
            "defect", "Mangel",
            "missing", "Fehlt",
            "costShare", "Kostenanteil",
            "confirmations", "Bestaetigungen",
            "signatures", "Unterschriften",
            "cityPrefix", "Ort:",
            "page", "Seite",
            "photoIndex", "Bildverzeichnis"
        ));
        all.put("en", l(
            "datePrefix", "on",
            "outTenant", "Moving out Tenant",
            "inTenant", "Moving in Tenant",
            "firstName", "First Name",
            "name", "Name",
            "address", "Address",
            "postalCode", "Postal Code",
            "city", "City",
            "phone", "Phone",
            "email", "Email",
            "property", "Property",
            "objectNumber", "Object No.",
            "rentalObject", "Rental Object",
            "incomingParty", "Incoming Party",
            "detail", "Detail",
            "text", "Text",
            "photos", "Photos",
            "new", "New",
            "normal", "Normal",
            "defect", "Defect",
            "missing", "Missing",
            "costShare", "Cost share",
            "confirmations", "Confirmations",
            "signatures", "Signatures",
            "cityPrefix", "City:",
            "page", "Page",
            "photoIndex", "Photo Index"
        ));
        all.put("fr", l(
            "datePrefix", "du",
            "outTenant", "Locataire sortant",
            "inTenant", "Locataire entrant",
            "firstName", "Prénom",
            "name", "Nom",
            "address", "Adresse",
            "postalCode", "Code postal",
            "city", "Ville",
            "phone", "Téléphone",
            "email", "Email",
            "property", "Bien",
            "objectNumber", "N° objet",
            "rentalObject", "Objet loué",
            "incomingParty", "Partie entrante",
            "detail", "Détail",
            "text", "Texte",
            "photos", "Photos",
            "new", "Neuf",
            "normal", "Normal",
            "defect", "Défaut",
            "missing", "Manquant",
            "costShare", "Quote-part",
            "confirmations", "Confirmations",
            "signatures", "Signatures",
            "cityPrefix", "Ville:",
            "page", "Page",
            "photoIndex", "Index des photos"
        ));
        all.put("it", l(
            "datePrefix", "del",
            "outTenant", "Inquilino in uscita",
            "inTenant", "Inquilino in entrata",
            "firstName", "Nome",
            "name", "Cognome",
            "address", "Indirizzo",
            "postalCode", "CAP",
            "city", "Città",
            "phone", "Telefono",
            "email", "Email",
            "property", "Proprietà",
            "objectNumber", "N. oggetto",
            "rentalObject", "Oggetto locato",
            "incomingParty", "Parte entrante",
            "detail", "Dettaglio",
            "text", "Testo",
            "photos", "Foto",
            "new", "Nuovo",
            "normal", "Normale",
            "defect", "Difetto",
            "missing", "Mancante",
            "costShare", "Quota parte",
            "confirmations", "Conferme",
            "signatures", "Firme",
            "cityPrefix", "Città:",
            "page", "Pagina",
            "photoIndex", "Indice foto"
        ));
        all.put("ro", l(
            "datePrefix", "din",
            "outTenant", "Chirias care se muta afara",
            "inTenant", "Chirias care se muta in",
            "firstName", "Prenume",
            "name", "Nume",
            "address", "Adresa",
            "postalCode", "Cod postal",
            "city", "Oras",
            "phone", "Telefon",
            "email", "Email",
            "property", "Proprietate",
            "objectNumber", "Nr. obiect",
            "rentalObject", "Obiect inchiriere",
            "incomingParty", "Partea care intra",
            "detail", "Detaliu",
            "text", "Text",
            "photos", "Foto",
            "new", "Nou",
            "normal", "Normal",
            "defect", "Defect",
            "missing", "Lipseste",
            "costShare", "Cota parte",
            "confirmations", "Confirmari",
            "signatures", "Semnaturi",
            "cityPrefix", "Oras:",
            "page", "Pagina",
            "photoIndex", "Index foto"
        ));
        return all;
    }

    private static Map<String, String> l(String... kvs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put(kvs[i], kvs[i + 1]);
        }
        return m;
    }
}
