package com.apartmentmanager.service;

import com.apartmentmanager.model.OcrKeywords;
import com.apartmentmanager.repository.OcrKeywordsRepository;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "([€$]|[Rr][Oo][Nn]|[Ll]ei|RON|EUR|USD|GBP|CHF|[Cc][Hh][Ff])",
        Pattern.CASE_INSENSITIVE
    );

    private final String tessDataPath;
    private final String tessLanguage;
    private final OcrKeywordsRepository ocrKeywordsRepository;

    public OcrService(
            @Value("${ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}") String tessDataPath,
            @Value("${ocr.language:deu+eng}") String tessLanguage,
            OcrKeywordsRepository ocrKeywordsRepository) {
        this.tessDataPath = tessDataPath;
        this.tessLanguage = tessLanguage;
        this.ocrKeywordsRepository = ocrKeywordsRepository;
    }

    private Pattern buildAmountPattern() {
        List<String> allKeywords = new ArrayList<>();
        List<OcrKeywords> all = ocrKeywordsRepository.findAll();
        for (OcrKeywords kw : all) {
            if (kw.getAmountKeywords() != null && !kw.getAmountKeywords().isEmpty()) {
                allKeywords.addAll(Arrays.asList(kw.getAmountKeywords().split("\\|")));
            }
        }
        if (allKeywords.isEmpty()) {
            allKeywords.addAll(Arrays.asList("total", "amount", "summe", "betrag", "gesamt",
                "totale", "importo", "montant", "total\\s*due", "payment\\s*amount",
                "total\\s*de\\s*achitat", "de\\s*achitat", "suma\\s*de\\s*pl[aă]tit", "total\\s*de\\s*plat[aă]"));
        }
        String keywordGroup = String.join("|", allKeywords);
        return Pattern.compile(
            "(?:" + keywordGroup + ")" +
            "[\\s\\r\\n:€$]*(?:([€$]|[Rr][Oo][Nn]|[Ll]ei|RON|EUR|USD|GBP|CHF|[Cc][Hh][Ff])\\s*)?" +
            "(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})?)" +
            "(?:\\s*([€$]|[Rr][Oo][Nn]|[Ll]ei|RON|EUR|USD|GBP|CHF|[Cc][Hh][Ff]))?",
            Pattern.CASE_INSENSITIVE
        );
    }

    public OcrResult analyze(Path filePath, String contentType) throws IOException {
        if (contentType == null) {
            contentType = Files.probeContentType(filePath);
        }

        try {
            if (contentType != null && contentType.equals("application/pdf")) {
                return analyzePdf(filePath);
            } else {
                return analyzeImage(filePath);
            }
        } catch (TesseractException e) {
            throw new IOException("OCR processing failed", e);
        }
    }



    private OcrResult analyzePdf(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            String text = extractPdfText(document);
            log.info("PDF text extraction: {} chars extracted", text != null ? text.length() : 0);
            if (text != null && text.length() > 0) {
                log.debug("Extracted text preview: {}", text.substring(0, Math.min(500, text.length())));
            }

            if (text != null && text.trim().length() > 10) {
                OcrResult result = parseAmountFromText(text, 0.95);
                log.info("Amount parsing result: amount={}, confidence={}", result.getAmount(), result.getConfidence());
                return result;
            }

            log.info("PDF text too short or empty, trying embedded images...");
            for (PDPage page : document.getPages()) {
                String imageText = extractTextFromEmbeddedImages(page);
                if (imageText != null && imageText.trim().length() > 10) {
                    log.info("Found text in embedded images: {} chars", imageText.length());
                    return parseAmountFromText(imageText, 0.70);
                }
            }

            log.info("No embedded images with text, falling back to full page OCR...");
            return ocrPdfPages(document);
        } catch (TesseractException e) {
            throw new IOException("OCR processing failed", e);
        }
    }

    private String extractPdfText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    }

    private String extractTextFromEmbeddedImages(PDPage page) throws IOException {
        StringBuilder allText = new StringBuilder();
        PDResources resources = page.getResources();

        if (resources == null) return null;

        for (COSName xobjectName : resources.getXObjectNames()) {
            try {
                PDXObject xobject = resources.getXObject(xobjectName);
                if (xobject instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image) {
                    BufferedImage bufferedImage = image.getImage();
                    File tempImage = Files.createTempFile("ocr_", ".png").toFile();
                    ImageIO.write(bufferedImage, "png", tempImage);

                    ITesseract tesseract = getTesseract();
                    String tesseractText = tesseract.doOCR(tempImage);
                    tempImage.delete();

                    if (tesseractText != null && tesseractText.trim().length() > 5) {
                        allText.append(tesseractText).append(" ");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to process XObject {}: {}", xobjectName.getName(), e.getMessage());
            }
        }

        return allText.toString().trim();
    }

    private OcrResult ocrPdfPages(PDDocument document) throws IOException, TesseractException {
        StringBuilder allText = new StringBuilder();
        PDFRenderer renderer = new PDFRenderer(document);

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage pageImage = renderer.renderImageWithDPI(i, 200);
            if (pageImage != null) {
                File tempImage = Files.createTempFile("ocr_page_", ".png").toFile();
                ImageIO.write(pageImage, "png", tempImage);

                ITesseract tesseract = getTesseract();
                String text = tesseract.doOCR(tempImage);
                tempImage.delete();

                if (text != null && text.trim().length() > 0) {
                    allText.append(text).append("\n");
                }
            }
        }

        if (allText.toString().trim().isEmpty()) {
            return new OcrResult(null, null, 0.0);
        }

        return parseAmountFromText(allText.toString(), 0.60);
    }

    private OcrResult analyzeImage(Path filePath) throws IOException, TesseractException {
        ITesseract tesseract = getTesseract();
        File imageFile = filePath.toFile();
        String text = tesseract.doOCR(imageFile);

        if (text == null || text.trim().isEmpty()) {
            return new OcrResult(null, null, 0.0);
        }

        return parseAmountFromText(text, 0.80);
    }

    private OcrResult parseAmountFromText(String text, double baseConfidence) {
        Pattern amountPattern = buildAmountPattern();
        Matcher matcher = amountPattern.matcher(text);

        if (matcher.find()) {
            String currency = matcher.group(1);
            String amountStr = matcher.group(2);
            String currencyAfter = matcher.group(3);
            if (currency == null || currency.isEmpty()) currency = currencyAfter;
            currency = normalizeCurrency(currency);
            if (currency == null) currency = inferCurrencyFromLanguage(text);
            log.info("Amount pattern matched: '{}' currency='{}' from text", amountStr, currency);
            Double amount = parseGermanNumber(amountStr);

            if (amount != null) {
                double confidence = Math.min(baseConfidence, 0.99);
                log.info("Parsed amount: {} {} with confidence {}", amount, currency, confidence);
                return new OcrResult(amount, currency, confidence);
            }
        }

        Pattern minusPattern = Pattern.compile("-\\s*(\\d{1,3}(?:[.,]\\d{3})*[.,]\\d{2})");
        Matcher minusMatcher = minusPattern.matcher(text);
        if (minusMatcher.find()) {
            String amountStr = minusMatcher.group(1);
            log.info("Negative amount pattern matched: '{}'", amountStr);
            Double amount = parseGermanNumber(amountStr);
            if (amount != null && amount > 0 && amount < 100000) {
                String currency = detectCurrencyNearAmount(text, amountStr);
                double confidence = Math.min(baseConfidence * 0.8, 0.70);
                log.info("Parsed negative amount: {} {} with confidence {}", amount, currency, confidence);
                return new OcrResult(-amount, currency, confidence);
            }
        }

        Pattern fallbackPattern = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*[.,]\\d{2})");
        Matcher fallback = fallbackPattern.matcher(text);

        if (fallback.find()) {
            String amountStr = fallback.group(1);
            log.info("Fallback pattern matched: '{}'", amountStr);
            Double amount = parseGermanNumber(amountStr);

            if (amount != null && amount > 0 && amount < 100000) {
                double confidence = Math.min(baseConfidence * 0.7, 0.60);
                String currency = detectCurrencyNearAmount(text, amountStr);
                log.info("Fallback amount: {} {} with confidence {}", amount, currency, confidence);
                return new OcrResult(amount, currency, confidence);
            }
        }

        log.info("No amount found in text ({} chars)", text.length());
        return new OcrResult(null, null, baseConfidence * 0.3);
    }

    private String detectCurrencyNearAmount(String text, String amountStr) {
        int idx = text.indexOf(amountStr);
        if (idx < 0) return inferCurrencyFromLanguage(text);
        String before = text.substring(Math.max(0, idx - 20), idx);
        String after = text.substring(idx + amountStr.length(), Math.min(text.length(), idx + amountStr.length() + 20));
        Matcher m = CURRENCY_PATTERN.matcher(after);
        if (m.find()) return normalizeCurrency(m.group(1));
        m = CURRENCY_PATTERN.matcher(before);
        if (m.find()) return normalizeCurrency(m.group(1));
        return inferCurrencyFromLanguage(text);
    }

    private String inferCurrencyFromLanguage(String text) {
        String lower = text.toLowerCase();
        Map<String, Integer> scores = new LinkedHashMap<>();

        List<OcrKeywords> all = ocrKeywordsRepository.findAll();
        for (OcrKeywords kw : all) {
            int score = 0;
            if (kw.getLanguageKeywords() != null && !kw.getLanguageKeywords().isEmpty()) {
                for (String keyword : kw.getLanguageKeywords().split("\\|")) {
                    if (lower.contains(keyword.trim().toLowerCase())) {
                        score += 2;
                    }
                }
            }
            if (kw.getDefaultCurrency() != null) {
                String currLower = kw.getDefaultCurrency().toLowerCase();
                if (lower.contains(currLower) || lower.contains(currLower + " ") ||
                    lower.contains(kw.getDefaultCurrency())) {
                    score += 3;
                }
            }
            scores.put(kw.getDefaultCurrency() != null ? kw.getDefaultCurrency() : "EUR", score);
        }

        if (scores.isEmpty()) {
            return "EUR";
        }

        String bestCurrency = "EUR";
        int bestScore = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            log.info("Language detection: {}={}", entry.getKey(), entry.getValue());
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCurrency = entry.getKey();
            }
        }

        return bestCurrency;
    }

    private String normalizeCurrency(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String lower = raw.toLowerCase();
        if (lower.equals("€") || lower.equals("eur")) return "EUR";
        if (lower.equals("$") || lower.equals("usd")) return "USD";
        if (lower.equals("lei") || lower.equals("ron") || lower.equals("r\non")) return "RON";
        if (lower.equals("gbp") || lower.equals("£")) return "GBP";
        if (lower.equals("chf")) return "CHF";
        return raw.toUpperCase();
    }

    private Double parseGermanNumber(String str) {
        if (str == null || str.isEmpty()) return null;

        str = str.replace(" ", "");

        if (str.contains(",") && str.contains(".")) {
            if (str.lastIndexOf(",") > str.lastIndexOf(".")) {
                str = str.replace(".", "").replace(",", ".");
            } else {
                str = str.replace(",", "");
            }
        } else if (str.contains(",")) {
            str = str.replace(",", ".");
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ITesseract getTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(tessLanguage);
        tesseract.setPageSegMode(6);
        return tesseract;
    }

    public static class OcrResult {
        private final Double amount;
        private final String currency;
        private final double confidence;

        public OcrResult(Double amount, String currency, double confidence) {
            this.amount = amount;
            this.currency = currency;
            this.confidence = confidence;
        }

        public Double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public double getConfidence() { return confidence; }
    }
}
