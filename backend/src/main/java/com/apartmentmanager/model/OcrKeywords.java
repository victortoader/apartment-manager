package com.apartmentmanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ocr_keywords")
public class OcrKeywords {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String language;

    private String amountKeywords;

    private String languageKeywords;

    private String paymentKeywords;

    private String defaultCurrency;

    public OcrKeywords() {}

    public OcrKeywords(String language, String amountKeywords, String languageKeywords, String paymentKeywords, String defaultCurrency) {
        this.language = language;
        this.amountKeywords = amountKeywords;
        this.languageKeywords = languageKeywords;
        this.paymentKeywords = paymentKeywords;
        this.defaultCurrency = defaultCurrency;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getAmountKeywords() { return amountKeywords; }
    public void setAmountKeywords(String amountKeywords) { this.amountKeywords = amountKeywords; }

    public String getLanguageKeywords() { return languageKeywords; }
    public void setLanguageKeywords(String languageKeywords) { this.languageKeywords = languageKeywords; }

    public String getPaymentKeywords() { return paymentKeywords; }
    public void setPaymentKeywords(String paymentKeywords) { this.paymentKeywords = paymentKeywords; }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
