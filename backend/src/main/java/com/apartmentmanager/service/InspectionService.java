package com.apartmentmanager.service;

import com.apartmentmanager.model.*;
import com.apartmentmanager.repository.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final InspectionSectionRepository sectionRepository;
    private final InspectionRowRepository rowRepository;
    private final InspectionRowPhotoRepository photoRepository;
    private final HandoverProtocolRepository protocolRepository;
    private final PhotoStorageService photoStorage;
    private final ApartmentService apartmentService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MT = 51f, MR = 45f, MB = 51f, ML = 45f;
    private static final float CW = PAGE_W - ML - MR;

    private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Transactional
    public Inspection create(Long apartmentId, Inspection inspection) {
        Apartment apartment = apartmentService.findById(apartmentId);
        inspection.setApartment(apartment);
        inspection.setCreatedAt(LocalDateTime.now());
        inspection.setUpdatedAt(LocalDateTime.now());
        if (inspection.getSections() != null) {
            for (int i = 0; i < inspection.getSections().size(); i++) {
                InspectionSection s = inspection.getSections().get(i);
                s.setInspection(inspection);
                s.setSortOrder(i);
                if (s.getRows() != null) {
                    for (int j = 0; j < s.getRows().size(); j++) {
                        InspectionRow r = s.getRows().get(j);
                        r.setSection(s);
                        r.setSortOrder(j);
                    }
                }
            }
        }
        return inspectionRepository.save(inspection);
    }

    public Inspection getById(Long id) {
        return inspectionRepository.findById(id).orElseThrow(() -> new RuntimeException("Inspection not found"));
    }

    public List<Inspection> getByApartment(Long apartmentId) {
        return inspectionRepository.findByApartmentIdOrderByCreatedAtDesc(apartmentId);
    }

    @Transactional
    public Inspection update(Long id, Inspection update) {
        Inspection existing = getById(id);
        if (update.getDocumentType() != null) existing.setDocumentType(update.getDocumentType());
        if (update.getDate() != null) existing.setDate(update.getDate());
        if (update.getTime() != null) existing.setTime(update.getTime());
        if (update.getCompanyName() != null) existing.setCompanyName(update.getCompanyName());
        if (update.getFirstName() != null) existing.setFirstName(update.getFirstName());
        if (update.getPreviousName() != null) existing.setPreviousName(update.getPreviousName());
        if (update.getPreviousFirstName() != null) existing.setPreviousFirstName(update.getPreviousFirstName());
        if (update.getPreviousAddress() != null) existing.setPreviousAddress(update.getPreviousAddress());
        if (update.getPreviousPostalCode() != null) existing.setPreviousPostalCode(update.getPreviousPostalCode());
        if (update.getPreviousCity() != null) existing.setPreviousCity(update.getPreviousCity());
        if (update.getPreviousPhone() != null) existing.setPreviousPhone(update.getPreviousPhone());
        if (update.getPreviousEmail() != null) existing.setPreviousEmail(update.getPreviousEmail());
        if (update.getCompanyAddress() != null) existing.setCompanyAddress(update.getCompanyAddress());
        if (update.getCompanyPostalCode() != null) existing.setCompanyPostalCode(update.getCompanyPostalCode());
        if (update.getCompanyCity() != null) existing.setCompanyCity(update.getCompanyCity());
        if (update.getCompanyPhone() != null) existing.setCompanyPhone(update.getCompanyPhone());
        if (update.getCompanyEmail() != null) existing.setCompanyEmail(update.getCompanyEmail());
        if (update.getProperty() != null) existing.setProperty(update.getProperty());
        if (update.getObjectNumber() != null) existing.setObjectNumber(update.getObjectNumber());
        if (update.getRentalObject() != null) existing.setRentalObject(update.getRentalObject());
        if (update.getIncomingParty() != null) existing.setIncomingParty(update.getIncomingParty());
        if (update.getConfirmationsJson() != null) existing.setConfirmationsJson(update.getConfirmationsJson());
        if (update.getSignaturesJson() != null) existing.setSignaturesJson(update.getSignaturesJson());
        if (update.getSections() != null) {
            Set<Long> updateIds = update.getSections().stream()
                .map(InspectionSection::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            existing.getSections().removeIf(s -> !updateIds.contains(s.getId()));
            for (int i = 0; i < update.getSections().size(); i++) {
                InspectionSection us = update.getSections().get(i);
                if (us.getId() != null) {
                    InspectionSection es = existing.getSections().stream()
                        .filter(s -> us.getId().equals(s.getId())).findFirst().orElse(null);
                    if (es != null) {
                        es.setTitle(us.getTitle());
                        es.setSortOrder(i);
                        mergeRows(es, us);
                    }
                } else {
                    us.setInspection(existing);
                    us.setSortOrder(i);
                    if (us.getRows() != null) {
                        for (int j = 0; j < us.getRows().size(); j++) {
                            InspectionRow r = us.getRows().get(j);
                            r.setId(null);
                            r.setSection(us);
                            r.setSortOrder(j);
                            if (r.getPhotos() != null && !r.getPhotos().isEmpty()) {
                                List<InspectionRowPhoto> reattached = new ArrayList<>();
                                for (InspectionRowPhoto ph : r.getPhotos()) {
                                    if (ph.getId() == null) continue;
                                    InspectionRowPhoto dbPhoto = photoRepository.findById(ph.getId()).orElse(null);
                                    if (dbPhoto != null) {
                                        dbPhoto.setSortOrder(ph.getSortOrder());
                                        dbPhoto.setRow(r);
                                        reattached.add(dbPhoto);
                                    }
                                }
                                r.setPhotos(reattached);
                            }
                        }
                    }
                    existing.getSections().add(us);
                }
            }
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return inspectionRepository.save(existing);
    }

    private void mergeRows(InspectionSection es, InspectionSection us) {
        Set<Long> updateRowIds = us.getRows().stream()
            .map(InspectionRow::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        es.getRows().removeIf(r -> !updateRowIds.contains(r.getId()));
        for (int j = 0; j < us.getRows().size(); j++) {
            InspectionRow ur = us.getRows().get(j);
            if (ur.getId() != null) {
                InspectionRow er = es.getRows().stream()
                    .filter(r -> ur.getId().equals(r.getId())).findFirst().orElse(null);
                if (er != null) {
                    er.setDetail(ur.getDetail());
                    er.setText(ur.getText());
                    er.setStatus(ur.getStatus());
                    er.setCostShare(ur.getCostShare());
                    er.setSortOrder(j);
                }
            } else {
                ur.setId(null);
                ur.setSection(es);
                ur.setSortOrder(j);
                es.getRows().add(ur);
            }
        }
    }

    @Transactional
    public InspectionSection addSection(Long inspectionId, InspectionSection section) {
        Inspection inspection = getById(inspectionId);
        List<InspectionSection> existing = sectionRepository.findByInspectionIdOrderBySortOrder(inspectionId);
        section.setSortOrder(existing.size());
        section.setInspection(inspection);
        return sectionRepository.save(section);
    }

    @Transactional
    public InspectionSection updateSection(Long sectionId, InspectionSection update) {
        InspectionSection section = sectionRepository.findById(sectionId).orElseThrow();
        if (update.getTitle() != null) section.setTitle(update.getTitle());
        if (update.getRows() != null) {
            section.getRows().clear();
            for (int i = 0; i < update.getRows().size(); i++) {
                InspectionRow r = update.getRows().get(i);
                r.setSection(section);
                r.setSortOrder(i);
                section.getRows().add(r);
            }
        }
        return sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        sectionRepository.deleteById(sectionId);
    }

    @Transactional
    public InspectionRow addRow(Long sectionId, InspectionRow row) {
        InspectionSection section = sectionRepository.findById(sectionId).orElseThrow();
        List<InspectionRow> existing = rowRepository.findBySectionIdOrderBySortOrder(sectionId);
        row.setSortOrder(existing.size());
        row.setSection(section);
        return rowRepository.save(row);
    }

    @Transactional
    public InspectionRow updateRow(Long rowId, InspectionRow update) {
        InspectionRow row = rowRepository.findById(rowId).orElseThrow();
        if (update.getDetail() != null) row.setDetail(update.getDetail());
        if (update.getText() != null) row.setText(update.getText());
        if (update.getStatus() != null) row.setStatus(update.getStatus());
        if (update.getCostShare() != null) row.setCostShare(update.getCostShare());
        return rowRepository.save(row);
    }

    @Transactional
    public void deleteRow(Long rowId) {
        rowRepository.deleteById(rowId);
    }

    @Transactional
    public InspectionRowPhoto uploadPhoto(Long rowId, MultipartFile file) throws IOException {
        InspectionRow row = rowRepository.findById(rowId).orElseThrow();
        String storedName = photoStorage.store(file);
        InspectionRowPhoto photo = new InspectionRowPhoto();
        photo.setStoredFileName(storedName);
        photo.setOriginalFileName(file.getOriginalFilename());
        photo.setSortOrder(row.getPhotos().size());
        photo.setRow(row);
        return photoRepository.save(photo);
    }

    @Transactional
    public void deletePhoto(Long photoId) {
        photoRepository.deleteById(photoId);
    }

    @Transactional
    public void delete(Long id) {
        inspectionRepository.deleteById(id);
    }

    @Transactional
    public HandoverProtocol generatePdf(Long inspectionId, Map<String, String> labelMap) throws IOException {
        Inspection inspection = getById(inspectionId);
        validateSignatures(inspection);
        PdfLabels labels = new PdfLabels(labelMap);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            List<PDPage> pages = new ArrayList<>();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            pages.add(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = PAGE_H - MT;

            y = drawTitle(cs, inspection, y, labels);
            y -= 10;
            y = drawTenantSections(cs, inspection, y, labels);
            y -= 8;
            y = drawPropertyMetadata(cs, inspection, y, labels);
            y -= 8;

            List<PhotoEntry> allPhotos = new ArrayList<>();
            int photoCounter = 1;

            if (inspection.getSections() != null) {
                for (InspectionSection section : inspection.getSections()) {
                    if (y < 120) { y = addPage(doc, pages, cs); cs = new PDPageContentStream(doc, pages.get(pages.size()-1)); }
                    y = drawSectionTitle(cs, section.getTitle(), y);
                    y -= 4;
                    float[] cw = cw();
                    y = drawTableHeader(cs, y, cw, labels);
                    if (section.getRows() != null) {
                        for (InspectionRow row : section.getRows()) {
                            if (y < 80) { y = addPage(doc, pages, cs); cs = new PDPageContentStream(doc, pages.get(pages.size()-1)); y = drawTableHeader(cs, y, cw, labels); }
                            List<Integer> refs = new ArrayList<>();
                            if (row.getPhotos() != null) {
                                for (InspectionRowPhoto ph : row.getPhotos()) {
                                    refs.add(photoCounter);
                                    allPhotos.add(new PhotoEntry(photoCounter, section.getTitle(), row.getDetail(), ph.getStoredFileName()));
                                    photoCounter++;
                                }
                            }
                            y = drawTableRow(cs, row, y, cw, refs);
                            y -= 2;
                        }
                        if (section.getRows().isEmpty()) {
                            if (y < 80) { y = addPage(doc, pages, cs); cs = new PDPageContentStream(doc, pages.get(pages.size()-1)); y = drawTableHeader(cs, y, cw, labels); }
                            y = drawTableRow(cs, new InspectionRow(), y, cw, Collections.emptyList());
                            y -= 2;
                        }
                    }
                    y -= 6;
                }
            }

            y -= 8;
            if (y < 140) { y = addPage(doc, pages, cs); cs = new PDPageContentStream(doc, pages.get(pages.size()-1)); }
            y = drawConfirmations(cs, inspection, y, labels);
            y -= 12;
            cs.close();
            drawSignatures(doc, pages, inspection, y, labels);

            if (!allPhotos.isEmpty()) {
                addPhotoAppendix(doc, allPhotos, pages, labels);
            }
            drawFooter(doc, pages, labels);
            doc.save(baos);
        }

        String storedName = "inspection-" + inspectionId + "-" + UUID.randomUUID() + ".pdf";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Files.write(uploadPath.resolve(storedName), baos.toByteArray());

        Apartment apartment = inspection.getApartment();
        HandoverProtocol protocol = new HandoverProtocol(storedName,
                "Uebergabeprotokoll_" + inspection.getDate() + ".pdf", "application/pdf",
                DocumentType.HANDOVER_PROTOCOL, apartment);
        protocol = protocolRepository.save(protocol);
        inspection.setGeneratedProtocolId(protocol.getId());
        inspectionRepository.save(inspection);
        return protocol;
    }

    @Transactional
    public byte[] downloadPhotosZip(Long inspectionId) throws IOException {
        Inspection inspection = getById(inspectionId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int idx = 1;
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            if (inspection.getSections() != null) {
                for (InspectionSection section : inspection.getSections()) {
                    if (section.getRows() != null) {
                        for (InspectionRow row : section.getRows()) {
                            if (row.getPhotos() != null) {
                                for (InspectionRowPhoto ph : row.getPhotos()) {
                                    Path file = photoStorage.load(ph.getStoredFileName());
                                    if (Files.exists(file)) {
                                        String name = String.format("%02d_%s", idx, safeZipName(ph.getOriginalFileName()));
                                        zos.putNextEntry(new ZipEntry(name));
                                        Files.copy(file, zos);
                                        zos.closeEntry();
                                        idx++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private String safeZipName(String original) {
        if (original == null || original.isBlank()) return "photo";
        String name = original.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
        return name.isBlank() ? "photo" : name;
    }

    private float addPage(PDDocument doc, List<PDPage> pages, PDPageContentStream cs) throws IOException {
        cs.close();
        PDPage p = new PDPage(PDRectangle.A4);
        doc.addPage(p);
        pages.add(p);
        return PAGE_H - MT;
    }

    private float drawTitle(PDPageContentStream cs, Inspection i, float y, PdfLabels labels) throws IOException {
        String t = (i.getDocumentType() != null ? i.getDocumentType() : "Uebergabeprotokoll")
                + " " + labels.get("datePrefix") + " " + (i.getDate() != null ? i.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "")
                + ", " + (i.getTime() != null ? i.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        cs.beginText(); cs.setFont(FONT_BOLD, 16); cs.newLineAtOffset(ML, y); cs.showText(t); cs.endText();
        return y - 22;
    }

    private float drawTenantSections(PDPageContentStream cs, Inspection i, float y, PdfLabels labels) throws IOException {
        String[][] fieldsOut = {
            {labels.get("firstName"), nn(i.getPreviousFirstName())},
            {labels.get("name"), nn(i.getPreviousName())},
            {labels.get("address"), nn(i.getPreviousAddress())},
            {labels.get("postalCode"), nn(i.getPreviousPostalCode())},
            {labels.get("city"), nn(i.getPreviousCity())},
            {labels.get("phone"), nn(i.getPreviousPhone())},
            {labels.get("email"), nn(i.getPreviousEmail())}
        };
        String[][] fieldsIn = {
            {labels.get("firstName"), nn(i.getFirstName())},
            {labels.get("name"), nn(i.getCompanyName())},
            {labels.get("address"), nn(i.getCompanyAddress())},
            {labels.get("postalCode"), nn(i.getCompanyPostalCode())},
            {labels.get("city"), nn(i.getCompanyCity())},
            {labels.get("phone"), nn(i.getCompanyPhone())},
            {labels.get("email"), nn(i.getCompanyEmail())}
        };

        float half = CW / 2;
        float headingH = 16, rowH = 14;
        int rows = fieldsOut.length;

        float maxLabelW = 0;
        for (String[] f : fieldsOut) maxLabelW = Math.max(maxLabelW, swBold(f[0] + ":", 9));
        for (String[] f : fieldsIn) maxLabelW = Math.max(maxLabelW, swBold(f[0] + ":", 9));
        float labelW = Math.min(maxLabelW + 12, half - 60);

        float[] rowHeights = new float[rows];
        float totalH = headingH;
        for (int f = 0; f < rows; f++) {
            int lines = Math.max(wrapText(nn(fieldsOut[f][1]), 9f, half - labelW - 8).size(),
                                 wrapText(nn(fieldsIn[f][1]), 9f, half - labelW - 8).size());
            float h = Math.max(rowH, (lines - 1) * 11 + 13);
            rowHeights[f] = h;
            totalH += h;
        }

        for (int c = 0; c < 2; c++) {
            String[][] fields = c == 0 ? fieldsOut : fieldsIn;
            float x0 = ML + c * half;
            float tableTop = y;
            float tableBottom = y - totalH;

            cs.setStrokingColor(0.6f, 0.6f, 0.6f);
            cs.setLineWidth(0.5f);
            cs.addRect(x0, tableBottom, half, tableTop - tableBottom);
            cs.stroke();
            cs.moveTo(x0, tableTop - headingH); cs.lineTo(x0 + half, tableTop - headingH); cs.stroke();
            float sepY = tableTop - headingH;
            for (int f = 0; f < rows - 1; f++) {
                sepY -= rowHeights[f];
                cs.moveTo(x0, sepY); cs.lineTo(x0 + half, sepY); cs.stroke();
            }
            cs.moveTo(x0 + labelW, tableBottom); cs.lineTo(x0 + labelW, tableTop); cs.stroke();
            cs.setNonStrokingColor(0, 0, 0);

            cs.beginText(); cs.setFont(FONT_BOLD, 10);
            cs.newLineAtOffset(x0 + 4, tableTop - headingH + 4);
            cs.showText(c == 0 ? labels.get("outTenant") : labels.get("inTenant"));
            cs.endText();

            float ry = tableTop - headingH;
            for (int f = 0; f < rows; f++) {
                float cellH = rowHeights[f];
                cs.beginText(); cs.setFont(FONT_BOLD, 9);
                cs.newLineAtOffset(x0 + 4, ry - cellH + 4);
                cs.showText(fields[f][0] + ":");
                cs.endText();

                List<String> valueLines = wrapText(nn(fields[f][1]), 9f, half - labelW - 8);
                cs.beginText(); cs.setFont(FONT, 9); cs.setLeading(11);
                cs.newLineAtOffset(x0 + labelW + 4, ry - 9);
                for (String line : valueLines) { cs.showText(line); cs.newLine(); }
                cs.endText();

                ry -= cellH;
            }
        }
        return y - totalH;
    }

    private float drawPropertyMetadata(PDPageContentStream cs, Inspection i, float y, PdfLabels labels) throws IOException {
        String[][] f = {
            {labels.get("property"), nn(i.getProperty())},
            {labels.get("objectNumber"), nn(i.getObjectNumber())},
            {labels.get("rentalObject"), nn(i.getRentalObject())},
            {labels.get("incomingParty"), nn(i.getIncomingParty())}
        };
        float half = CW / 2;
        for (int n = 0; n < f.length; n++) {
            float x = n < 2 ? ML : ML + half;
            float ry = y - (n % 2) * 16;
            cs.beginText(); cs.setFont(FONT_BOLD, 9); cs.newLineAtOffset(x, ry); cs.showText(f[n][0] + ":"); cs.endText();
            cs.beginText(); cs.setFont(FONT, 9); cs.newLineAtOffset(x + 80, ry); cs.showText(f[n][1]); cs.endText();
        }
        return y - 36;
    }

    private float[] cw() {
        float w = CW;
        return new float[]{w*0.19f, w*0.37f, w*0.07f, w*0.07f, w*0.08f, w*0.07f, w*0.07f, w*0.08f};
    }

    private float drawSectionTitle(PDPageContentStream cs, String title, float y) throws IOException {
        cs.beginText(); cs.setFont(FONT_BOLD, 11); cs.newLineAtOffset(ML, y); cs.showText(title); cs.endText();
        return y - 16;
    }

    private float drawTableHeader(PDPageContentStream cs, float y, float[] cw, PdfLabels labels) throws IOException {
        String[] h = {labels.get("detail"), labels.get("text"), labels.get("photos"), labels.get("new"), labels.get("normal"), labels.get("defect"), labels.get("missing"), labels.get("costShare")};
        float x = ML, rh = 14;
        cs.setStrokingColor(0.74f, 0.74f, 0.74f);
        cs.setLineWidth(0.5f);
        for (int i = 0; i < h.length; i++) { cs.addRect(x, y - rh, cw[i], rh); x += cw[i]; }
        cs.stroke();
        x = ML;
        for (int i = 0; i < h.length; i++) {
            cs.beginText(); cs.setFont(FONT_BOLD, 7.5f); cs.setNonStrokingColor(0.33f, 0.33f, 0.33f);
            cs.newLineAtOffset(x + (cw[i] - sw(h[i], 7.5f)) / 2, y - rh + 4); cs.showText(h[i]); cs.endText();
            x += cw[i];
        }
        cs.setNonStrokingColor(0, 0, 0);
        return y - rh;
    }

    private float drawTableRow(PDPageContentStream cs, InspectionRow row, float y, float[] cw, List<Integer> refs) throws IOException {
        float x = ML;
        String txt = nn(row.getText());
        float rh = Math.max(18, txt.length() > 60 ? 18 + (txt.length() / 60) * 10 : 18);
        cs.setStrokingColor(0.74f, 0.74f, 0.74f); cs.setLineWidth(0.5f);
        for (int i = 0; i < cw.length; i++) { cs.addRect(x, y - rh, cw[i], rh); x += cw[i]; }
        cs.stroke();
        x = ML;

        cs.beginText(); cs.setFont(FONT, 8.5f); cs.newLineAtOffset(x + 3, y - 12); cs.showText(nn(row.getDetail())); cs.endText();
        x += cw[0];

        cs.beginText(); cs.setFont(FONT, 8.5f); cs.setLeading(10); cs.newLineAtOffset(x + 3, y - 12);
        float lw = 0;
        for (String w : txt.split(" ")) {
            float ww = sw(w + " ", 8.5f);
            if (lw + ww > cw[1] - 6) { cs.newLine(); lw = 0; }
            cs.showText(w + " "); lw += ww;
        }
        cs.endText();
        x += cw[1];

        String circled = refs.stream().map(this::circled).collect(Collectors.joining(" "));
        cs.beginText(); cs.setFont(FONT, 8); cs.newLineAtOffset(x + (cw[2] - sw(circled, 8)) / 2, y - 12); cs.showText(circled); cs.endText();
        x += cw[2];

        String st = row.getStatus() != null ? row.getStatus() : "";
        String[] statusKeys = {"new", "normal", "defect", "missing"};
        for (int i = 0; i < statusKeys.length; i++) {
            if (statusKeys[i].equals(st)) {
                cs.setLineWidth(1.2f);
                float cx = x + cw[3] / 2, cy = y - 8;
                cs.moveTo(cx - 3, cy - 1); cs.lineTo(cx, cy + 3); cs.lineTo(cx + 4, cy - 3);
                cs.stroke();
            }
            x += cw[3];
        }

        cs.beginText(); cs.setFont(FONT, 8.5f); cs.newLineAtOffset(x + 3, y - 12); cs.showText(nn(row.getCostShare())); cs.endText();
        return y - rh;
    }

    @SuppressWarnings("unchecked")
    private float drawConfirmations(PDPageContentStream cs, Inspection i, float y, PdfLabels labels) throws IOException {
        cs.beginText(); cs.setFont(FONT_BOLD, 11); cs.newLineAtOffset(ML, y); cs.showText(labels.get("confirmations")); cs.endText();
        y -= 16;
        if (i.getConfirmationsJson() != null && !i.getConfirmationsJson().isEmpty()) {
            try {
                List<String> confs = new tools.jackson.databind.ObjectMapper().readValue(i.getConfirmationsJson(), List.class);
                cs.beginText(); cs.setFont(FONT, 9); cs.setLeading(13); cs.newLineAtOffset(ML, y);
                for (String c : confs) { cs.showText("\u2022 " + c); cs.newLine(); y -= 13; }
                cs.endText();
            } catch (Exception ignored) {}
        }
        return y;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseSignatures(Inspection i) {
        List<Map<String, String>> sigs = new ArrayList<>();
        if (i.getSignaturesJson() != null && !i.getSignaturesJson().isEmpty()) {
            try {
                sigs.addAll(new tools.jackson.databind.ObjectMapper().readValue(i.getSignaturesJson(), List.class));
            } catch (Exception ignored) {}
        }
        return sigs;
    }

    private void validateSignatures(Inspection i) {
        List<Map<String, String>> sigs = parseSignatures(i);
        if (sigs.isEmpty()) {
            throw new IllegalStateException("Signatures are required before generating the PDF");
        }
        for (int n = 0; n < sigs.size(); n++) {
            String sig = sigs.get(n).get("signature");
            if (sig == null || sig.isBlank() || !sig.startsWith("data:image/")) {
                throw new IllegalStateException("All signatures must be drawn before generating the PDF");
            }
        }
    }

    private void drawSignatures(PDDocument doc, List<PDPage> pages, Inspection i, float y, PdfLabels labels) throws IOException {
        List<Map<String, String>> sigs = parseSignatures(i);
        if (sigs.isEmpty()) {
            sigs.add(hm("role", "Moving in tenant"));
            sigs.add(hm("role", "admin"));
        }

        float half = CW / 2;
        float blockH = 74;

        PDPageContentStream cs = new PDPageContentStream(doc, pages.get(pages.size() - 1),
                PDPageContentStream.AppendMode.APPEND, true, false);
        cs.beginText(); cs.setFont(FONT_BOLD, 11); cs.newLineAtOffset(ML, y); cs.showText(labels.get("signatures")); cs.endText();
        y -= 20;

        for (int n = 0; n < sigs.size(); n++) {
            if (n % 2 == 0 && n > 0 && y - blockH < MB + 40) {
                y = addPage(doc, pages, cs);
                cs = new PDPageContentStream(doc, pages.get(pages.size() - 1),
                        PDPageContentStream.AppendMode.APPEND, true, false);
            }
            Map<String, String> s = sigs.get(n);
            float sx = ML + (n % 2) * half;
            String role = s.getOrDefault("role", "");
            String name = s.getOrDefault("name", "");
            String city = s.getOrDefault("city", "");
            String info = String.join("  |  ", role, name, labels.get("cityPrefix") + " " + city);
            List<String> infoLines = wrapText(info, 8.5f, half - 12);
            cs.beginText(); cs.setFont(FONT, 8.5f); cs.setLeading(11);
            cs.newLineAtOffset(sx, y);
            for (String l : infoLines) { cs.showText(l); cs.newLine(); }
            cs.endText();
            float lineY = y - infoLines.size() * 11 - 4;
            cs.setStrokingColor(0.74f, 0.74f, 0.74f); cs.setLineWidth(0.5f);
            cs.moveTo(sx, lineY); cs.lineTo(sx + half - 20, lineY); cs.stroke();
            drawSignatureImage(cs, doc, s.get("signature"), sx, lineY, half - 20, 30);
            if (n % 2 == 1) y -= blockH;
        }
        cs.close();
    }

    private List<String> wrapText(String text, float size, float maxW) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return List.of("");
        StringBuilder cur = new StringBuilder();
        float w = 0;
        for (String word : text.split(" ")) {
            float ww = sw(word + " ", size);
            if (w + ww > maxW && cur.length() > 0) {
                lines.add(cur.toString().trim());
                cur = new StringBuilder();
                w = 0;
            }
            cur.append(word).append(' ');
            w += ww;
        }
        if (cur.length() > 0) lines.add(cur.toString().trim());
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void drawSignatureImage(PDPageContentStream cs, PDDocument doc, String dataUrl, float x, float lineY, float maxW, float maxH) throws IOException {
        if (dataUrl == null || dataUrl.isBlank() || !dataUrl.startsWith("data:image/")) return;
        int comma = dataUrl.indexOf(',');
        if (comma < 0) return;
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "signature");
            float scale = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
            float iw = img.getWidth() * scale;
            float ih = img.getHeight() * scale;
            cs.drawImage(img, x + (maxW - iw) / 2, lineY - ih, iw, ih);
        } catch (Exception ignored) {}
    }

    private void drawFooter(PDDocument doc, List<PDPage> pages, PdfLabels labels) throws IOException {
        for (int i = 0; i < pages.size(); i++) {
            PDPageContentStream fcs = new PDPageContentStream(doc, pages.get(i), PDPageContentStream.AppendMode.APPEND, true);
            String text = labels.get("page") + " " + (i + 1) + "/" + pages.size();
            fcs.beginText(); fcs.setFont(FONT, 7); fcs.newLineAtOffset((PAGE_W - sw(text, 7)) / 2, 20); fcs.showText(text); fcs.endText();
            fcs.close();
        }
    }

    private void addPhotoAppendix(PDDocument doc, List<PhotoEntry> photos, List<PDPage> pages, PdfLabels labels) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        pages.add(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        float y = PAGE_H - MT;
        cs.beginText(); cs.setFont(FONT_BOLD, 16); cs.newLineAtOffset(ML, y); cs.showText(labels.get("photoIndex")); cs.endText();
        y -= 30;

        for (PhotoEntry p : photos) {
            if (y < 120) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                pages.add(page);
                cs = new PDPageContentStream(doc, page);
                y = PAGE_H - MT - 10;
            }
            float imgH = 0;
            try {
                Path imgPath = photoStorage.load(p.storedFileName);
                if (Files.exists(imgPath)) {
                    PDImageXObject img = PDImageXObject.createFromFileByContent(imgPath.toFile(), doc);
                    float scale = Math.min(CW / img.getWidth(), (y - 60) / img.getHeight());
                    float imgW = img.getWidth() * scale;
                    imgH = img.getHeight() * scale;
                    cs.drawImage(img, ML + (CW - imgW) / 2, y - imgH, imgW, imgH);
                }
            } catch (Exception ignored) {}
            String caption = "(" + p.id + ") " + p.section;
            float capY = y - imgH - 14;
            cs.beginText(); cs.setFont(FONT, 8);
            cs.newLineAtOffset(ML + (CW - sw(caption, 8)) / 2, capY);
            cs.showText(caption);
            cs.endText();
            y -= imgH + 24;
        }
        cs.close();
    }

    private String circled(int n) {
        return "(" + n + ")";
    }

    private float sw(String text, float size) throws IOException {
        if (text == null || text.isEmpty()) return 0;
        return FONT.getStringWidth(text) / 1000f * size;
    }

    private float swBold(String text, float size) throws IOException {
        if (text == null || text.isEmpty()) return 0;
        return FONT_BOLD.getStringWidth(text) / 1000f * size;
    }

    private String nn(String s) { return s != null ? s : ""; }

    private Map<String, String> hm(String k, String v) {
        Map<String, String> m = new HashMap<>(); m.put(k, v); return m;
    }

    private static class PhotoEntry {
        int id; String section, detail, storedFileName;
        PhotoEntry(int id, String s, String d, String f) { this.id = id; this.section = s; this.detail = d; this.storedFileName = f; }
    }
}
