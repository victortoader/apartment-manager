package com.apartmentmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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

    public OcrKeywords(String language, String amountKeywords, String languageKeywords, String paymentKeywords, String defaultCurrency) {
        this.language = language;
        this.amountKeywords = amountKeywords;
        this.languageKeywords = languageKeywords;
        this.paymentKeywords = paymentKeywords;
        this.defaultCurrency = defaultCurrency;
    }
}
