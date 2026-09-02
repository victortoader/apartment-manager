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
    private static final float MARGIN_TOP = 51f, MARGIN_RIGHT = 45f, MARGIN_BOTTOM = 51f, MARGIN_LEFT = 45f;
    private static final float CONTENT_WIDTH = PAGE_W - MARGIN_LEFT - MARGIN_RIGHT;

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
        String storedName = photoStorage.storeImage(file);
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            List<PDPage> pages = new ArrayList<>();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            pages.add(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            float cursorY = PAGE_H - MARGIN_TOP;

            cursorY = drawTitle(contentStream, inspection, cursorY, labels);
            cursorY -= 10;
            cursorY = drawTenantSections(contentStream, inspection, cursorY, labels);
            cursorY -= 8;
            cursorY = drawPropertyMetadata(contentStream, inspection, cursorY, labels);
            cursorY -= 8;

            List<PhotoEntry> allPhotos = new ArrayList<>();
            int photoCounter = 1;

            if (inspection.getSections() != null) {
                for (InspectionSection section : inspection.getSections()) {
                    if (cursorY < 120) { cursorY = addPage(doc, pages, contentStream); contentStream = new PDPageContentStream(doc, pages.get(pages.size()-1)); }
                    cursorY = drawSectionTitle(contentStream, section.getTitle(), cursorY);
                    cursorY -= 4;
                    float[] columnWidths = tableColumnWidths();
                    cursorY = drawTableHeader(contentStream, cursorY, columnWidths, labels);
                    if (section.getRows() != null) {
                        for (InspectionRow row : section.getRows()) {
                            if (cursorY < 80) { cursorY = addPage(doc, pages, contentStream); contentStream = new PDPageContentStream(doc, pages.get(pages.size()-1)); cursorY = drawTableHeader(contentStream, cursorY, columnWidths, labels); }
                            List<Integer> refs = new ArrayList<>();
                            if (row.getPhotos() != null) {
                                for (InspectionRowPhoto ph : row.getPhotos()) {
                                    refs.add(photoCounter);
                                    allPhotos.add(new PhotoEntry(photoCounter, section.getTitle(), row.getDetail(), ph.getStoredFileName()));
                                    photoCounter++;
                                }
                            }
                            cursorY = drawTableRow(contentStream, row, cursorY, columnWidths, refs);
                            cursorY -= 2;
                        }
                        if (section.getRows().isEmpty()) {
                            if (cursorY < 80) { cursorY = addPage(doc, pages, contentStream); contentStream = new PDPageContentStream(doc, pages.get(pages.size()-1)); cursorY = drawTableHeader(contentStream, cursorY, columnWidths, labels); }
                            cursorY = drawTableRow(contentStream, new InspectionRow(), cursorY, columnWidths, Collections.emptyList());
                            cursorY -= 2;
                        }
                    }
                    cursorY -= 6;
                }
            }

            cursorY -= 8;
            if (cursorY < 140) { cursorY = addPage(doc, pages, contentStream); contentStream = new PDPageContentStream(doc, pages.get(pages.size()-1)); }
            cursorY = drawConfirmations(contentStream, inspection, cursorY, labels);
            cursorY -= 12;
            contentStream.close();
            drawSignatures(doc, pages, inspection, cursorY, labels);

            if (!allPhotos.isEmpty()) {
                addPhotoAppendix(doc, allPhotos, pages, labels);
            }
            drawFooter(doc, pages, labels);
            doc.save(outputStream);
        }

        String storedName = "inspection-" + inspectionId + "-" + UUID.randomUUID() + ".pdf";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Files.write(uploadPath.resolve(storedName), outputStream.toByteArray());

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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int idx = 1;
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
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
        return outputStream.toByteArray();
    }

    private String safeZipName(String original) {
        if (original == null || original.isBlank()) return "photo";
        String name = original.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
        return name.isBlank() ? "photo" : name;
    }

    private float addPage(PDDocument doc, List<PDPage> pages, PDPageContentStream contentStream) throws IOException {
        contentStream.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        pages.add(page);
        return PAGE_H - MARGIN_TOP;
    }

    private float drawTitle(PDPageContentStream contentStream, Inspection inspection, float cursorY, PdfLabels labels) throws IOException {
        String title = (inspection.getDocumentType() != null ? inspection.getDocumentType() : "Uebergabeprotokoll")
                + " " + labels.get("datePrefix") + " " + (inspection.getDate() != null ? inspection.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "")
                + ", " + (inspection.getTime() != null ? inspection.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        contentStream.beginText(); contentStream.setFont(FONT_BOLD, 16); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY); contentStream.showText(title); contentStream.endText();
        return cursorY - 22;
    }

    private float drawTenantSections(PDPageContentStream contentStream, Inspection inspection, float cursorY, PdfLabels labels) throws IOException {
        String[][] fieldsOut = {
            {labels.get("firstName"), nullToEmpty(inspection.getPreviousFirstName())},
            {labels.get("name"), nullToEmpty(inspection.getPreviousName())},
            {labels.get("address"), nullToEmpty(inspection.getPreviousAddress())},
            {labels.get("postalCode"), nullToEmpty(inspection.getPreviousPostalCode())},
            {labels.get("city"), nullToEmpty(inspection.getPreviousCity())},
            {labels.get("phone"), nullToEmpty(inspection.getPreviousPhone())},
            {labels.get("email"), nullToEmpty(inspection.getPreviousEmail())}
        };
        String[][] fieldsIn = {
            {labels.get("firstName"), nullToEmpty(inspection.getFirstName())},
            {labels.get("name"), nullToEmpty(inspection.getCompanyName())},
            {labels.get("address"), nullToEmpty(inspection.getCompanyAddress())},
            {labels.get("postalCode"), nullToEmpty(inspection.getCompanyPostalCode())},
            {labels.get("city"), nullToEmpty(inspection.getCompanyCity())},
            {labels.get("phone"), nullToEmpty(inspection.getCompanyPhone())},
            {labels.get("email"), nullToEmpty(inspection.getCompanyEmail())}
        };

        float halfContentWidth = CONTENT_WIDTH / 2;
        float headingHeight = 16, rowHeight = 14;
        int rows = fieldsOut.length;

        float maxLabelWidth = 0;
        for (String[] field : fieldsOut) maxLabelWidth = Math.max(maxLabelWidth, boldTextWidth(field[0] + ":", 9));
        for (String[] field : fieldsIn) maxLabelWidth = Math.max(maxLabelWidth, boldTextWidth(field[0] + ":", 9));
        float labelWidth = Math.min(maxLabelWidth + 12, halfContentWidth - 60);

        float[] rowHeights = new float[rows];
        float totalHeight = headingHeight;
        for (int fieldIndex = 0; fieldIndex < rows; fieldIndex++) {
            int lines = Math.max(wrapText(nullToEmpty(fieldsOut[fieldIndex][1]), 9f, halfContentWidth - labelWidth - 8).size(),
                                 wrapText(nullToEmpty(fieldsIn[fieldIndex][1]), 9f, halfContentWidth - labelWidth - 8).size());
            float cellHeight = Math.max(rowHeight, (lines - 1) * 11 + 13);
            rowHeights[fieldIndex] = cellHeight;
            totalHeight += cellHeight;
        }

        for (int column = 0; column < 2; column++) {
            String[][] fields = column == 0 ? fieldsOut : fieldsIn;
            float tableX = MARGIN_LEFT + column * halfContentWidth;
            float tableTop = cursorY;
            float tableBottom = cursorY - totalHeight;

            contentStream.setStrokingColor(0.6f, 0.6f, 0.6f);
            contentStream.setLineWidth(0.5f);
            contentStream.addRect(tableX, tableBottom, halfContentWidth, tableTop - tableBottom);
            contentStream.stroke();
            contentStream.moveTo(tableX, tableTop - headingHeight); contentStream.lineTo(tableX + halfContentWidth, tableTop - headingHeight); contentStream.stroke();
            float separatorY = tableTop - headingHeight;
            for (int fieldIndex = 0; fieldIndex < rows - 1; fieldIndex++) {
                separatorY -= rowHeights[fieldIndex];
                contentStream.moveTo(tableX, separatorY); contentStream.lineTo(tableX + halfContentWidth, separatorY); contentStream.stroke();
            }
            contentStream.moveTo(tableX + labelWidth, tableBottom); contentStream.lineTo(tableX + labelWidth, tableTop); contentStream.stroke();
            contentStream.setNonStrokingColor(0, 0, 0);

            contentStream.beginText(); contentStream.setFont(FONT_BOLD, 10);
            contentStream.newLineAtOffset(tableX + 4, tableTop - headingHeight + 4);
            contentStream.showText(column == 0 ? labels.get("outTenant") : labels.get("inTenant"));
            contentStream.endText();

            float rowY = tableTop - headingHeight;
            for (int fieldIndex = 0; fieldIndex < rows; fieldIndex++) {
                float cellHeight = rowHeights[fieldIndex];
                contentStream.beginText(); contentStream.setFont(FONT_BOLD, 9);
                contentStream.newLineAtOffset(tableX + 4, rowY - cellHeight + 4);
                contentStream.showText(fields[fieldIndex][0] + ":");
                contentStream.endText();

                List<String> valueLines = wrapText(nullToEmpty(fields[fieldIndex][1]), 9f, halfContentWidth - labelWidth - 8);
                contentStream.beginText(); contentStream.setFont(FONT, 9); contentStream.setLeading(11);
                contentStream.newLineAtOffset(tableX + labelWidth + 4, rowY - 9);
                for (String line : valueLines) { contentStream.showText(line); contentStream.newLine(); }
                contentStream.endText();

                rowY -= cellHeight;
            }
        }
        return cursorY - totalHeight;
    }

    private float drawPropertyMetadata(PDPageContentStream contentStream, Inspection inspection, float cursorY, PdfLabels labels) throws IOException {
        String[][] fields = {
            {labels.get("property"), nullToEmpty(inspection.getProperty())},
            {labels.get("objectNumber"), nullToEmpty(inspection.getObjectNumber())},
            {labels.get("rentalObject"), nullToEmpty(inspection.getRentalObject())},
            {labels.get("incomingParty"), nullToEmpty(inspection.getIncomingParty())}
        };
        float halfContentWidth = CONTENT_WIDTH / 2;
        for (int index = 0; index < fields.length; index++) {
            float cursorX = index < 2 ? MARGIN_LEFT : MARGIN_LEFT + halfContentWidth;
            float rowY = cursorY - (index % 2) * 16;
            contentStream.beginText(); contentStream.setFont(FONT_BOLD, 9); contentStream.newLineAtOffset(cursorX, rowY); contentStream.showText(fields[index][0] + ":"); contentStream.endText();
            contentStream.beginText(); contentStream.setFont(FONT, 9); contentStream.newLineAtOffset(cursorX + 80, rowY); contentStream.showText(fields[index][1]); contentStream.endText();
        }
        return cursorY - 36;
    }

    private float[] tableColumnWidths() {
        float contentWidth = CONTENT_WIDTH;
        return new float[]{contentWidth*0.19f, contentWidth*0.37f, contentWidth*0.07f, contentWidth*0.07f, contentWidth*0.08f, contentWidth*0.07f, contentWidth*0.07f, contentWidth*0.08f};
    }

    private float drawSectionTitle(PDPageContentStream contentStream, String title, float cursorY) throws IOException {
        contentStream.beginText(); contentStream.setFont(FONT_BOLD, 11); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY); contentStream.showText(title); contentStream.endText();
        return cursorY - 16;
    }

    private float drawTableHeader(PDPageContentStream contentStream, float cursorY, float[] columnWidths, PdfLabels labels) throws IOException {
        String[] headers = {labels.get("detail"), labels.get("text"), labels.get("photos"), labels.get("new"), labels.get("normal"), labels.get("defect"), labels.get("missing"), labels.get("costShare")};
        float cursorX = MARGIN_LEFT, rowHeight = 14;
        contentStream.setStrokingColor(0.74f, 0.74f, 0.74f);
        contentStream.setLineWidth(0.5f);
        for (int i = 0; i < headers.length; i++) { contentStream.addRect(cursorX, cursorY - rowHeight, columnWidths[i], rowHeight); cursorX += columnWidths[i]; }
        contentStream.stroke();
        cursorX = MARGIN_LEFT;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText(); contentStream.setFont(FONT_BOLD, 7.5f); contentStream.setNonStrokingColor(0.33f, 0.33f, 0.33f);
            contentStream.newLineAtOffset(cursorX + (columnWidths[i] - textWidth(headers[i], 7.5f)) / 2, cursorY - rowHeight + 4); contentStream.showText(headers[i]); contentStream.endText();
            cursorX += columnWidths[i];
        }
        contentStream.setNonStrokingColor(0, 0, 0);
        return cursorY - rowHeight;
    }

    private float drawTableRow(PDPageContentStream contentStream, InspectionRow row, float cursorY, float[] columnWidths, List<Integer> refs) throws IOException {
        float cursorX = MARGIN_LEFT;
        String text = nullToEmpty(row.getText());
        float rowHeight = Math.max(18, text.length() > 60 ? 18 + (text.length() / 60) * 10 : 18);
        contentStream.setStrokingColor(0.74f, 0.74f, 0.74f); contentStream.setLineWidth(0.5f);
        for (int i = 0; i < columnWidths.length; i++) { contentStream.addRect(cursorX, cursorY - rowHeight, columnWidths[i], rowHeight); cursorX += columnWidths[i]; }
        contentStream.stroke();
        cursorX = MARGIN_LEFT;

        contentStream.beginText(); contentStream.setFont(FONT, 8.5f); contentStream.newLineAtOffset(cursorX + 3, cursorY - 12); contentStream.showText(nullToEmpty(row.getDetail())); contentStream.endText();
        cursorX += columnWidths[0];

        contentStream.beginText(); contentStream.setFont(FONT, 8.5f); contentStream.setLeading(10); contentStream.newLineAtOffset(cursorX + 3, cursorY - 12);
        float lineWidth = 0;
        for (String word : text.split(" ")) {
            float wordWidth = textWidth(word + " ", 8.5f);
            if (lineWidth + wordWidth > columnWidths[1] - 6) { contentStream.newLine(); lineWidth = 0; }
            contentStream.showText(word + " "); lineWidth += wordWidth;
        }
        contentStream.endText();
        cursorX += columnWidths[1];

        String circled = refs.stream().map(this::circled).collect(Collectors.joining(" "));
        contentStream.beginText(); contentStream.setFont(FONT, 8); contentStream.newLineAtOffset(cursorX + (columnWidths[2] - textWidth(circled, 8)) / 2, cursorY - 12); contentStream.showText(circled); contentStream.endText();
        cursorX += columnWidths[2];

        String status = row.getStatus() != null ? row.getStatus() : "";
        String[] statusKeys = {"new", "normal", "defect", "missing"};
        for (int i = 0; i < statusKeys.length; i++) {
            if (statusKeys[i].equals(status)) {
                contentStream.setLineWidth(1.2f);
                float checkCenterX = cursorX + columnWidths[3] / 2, checkCenterY = cursorY - 8;
                contentStream.moveTo(checkCenterX - 3, checkCenterY - 1); contentStream.lineTo(checkCenterX, checkCenterY + 3); contentStream.lineTo(checkCenterX + 4, checkCenterY - 3);
                contentStream.stroke();
            }
            cursorX += columnWidths[3];
        }

        contentStream.beginText(); contentStream.setFont(FONT, 8.5f); contentStream.newLineAtOffset(cursorX + 3, cursorY - 12); contentStream.showText(nullToEmpty(row.getCostShare())); contentStream.endText();
        return cursorY - rowHeight;
    }

    @SuppressWarnings("unchecked")
    private float drawConfirmations(PDPageContentStream contentStream, Inspection inspection, float cursorY, PdfLabels labels) throws IOException {
        contentStream.beginText(); contentStream.setFont(FONT_BOLD, 11); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY); contentStream.showText(labels.get("confirmations")); contentStream.endText();
        cursorY -= 16;
        if (inspection.getConfirmationsJson() != null && !inspection.getConfirmationsJson().isEmpty()) {
            try {
                List<String> confirmations = new tools.jackson.databind.ObjectMapper().readValue(inspection.getConfirmationsJson(), List.class);
                contentStream.beginText(); contentStream.setFont(FONT, 9); contentStream.setLeading(13); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY);
                for (String confirmation : confirmations) { contentStream.showText("\u2022 " + confirmation); contentStream.newLine(); cursorY -= 13; }
                contentStream.endText();
            } catch (Exception ignored) {}
        }
        return cursorY;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseSignatures(Inspection inspection) {
        List<Map<String, String>> sigs = new ArrayList<>();
        if (inspection.getSignaturesJson() != null && !inspection.getSignaturesJson().isEmpty()) {
            try {
                sigs.addAll(new tools.jackson.databind.ObjectMapper().readValue(inspection.getSignaturesJson(), List.class));
            } catch (Exception ignored) {}
        }
        return sigs;
    }

    private void validateSignatures(Inspection inspection) {
        List<Map<String, String>> sigs = parseSignatures(inspection);
        if (sigs.isEmpty()) {
            throw new IllegalStateException("Signatures are required before generating the PDF");
        }
        for (int index = 0; index < sigs.size(); index++) {
            String sig = sigs.get(index).get("signature");
            if (sig == null || sig.isBlank() || !sig.startsWith("data:image/")) {
                throw new IllegalStateException("All signatures must be drawn before generating the PDF");
            }
            int commaIndex = sig.indexOf(',');
            if (commaIndex < 0) {
                throw new IllegalStateException("Invalid signature data format");
            }
            try {
                Base64.getDecoder().decode(sig.substring(commaIndex + 1));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid signature image data");
            }
        }
    }

    private void drawSignatures(PDDocument doc, List<PDPage> pages, Inspection inspection, float cursorY, PdfLabels labels) throws IOException {
        List<Map<String, String>> sigs = parseSignatures(inspection);
        if (sigs.isEmpty()) {
            sigs.add(singletonMap("role", "Moving in tenant"));
            sigs.add(singletonMap("role", "admin"));
        }

        float halfContentWidth = CONTENT_WIDTH / 2;
        float blockHeight = 74;

        PDPageContentStream contentStream = new PDPageContentStream(doc, pages.get(pages.size() - 1),
                PDPageContentStream.AppendMode.APPEND, true, false);
        contentStream.beginText(); contentStream.setFont(FONT_BOLD, 11); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY); contentStream.showText(labels.get("signatures")); contentStream.endText();
        cursorY -= 20;

        for (int index = 0; index < sigs.size(); index++) {
            if (index % 2 == 0 && index > 0 && cursorY - blockHeight < MARGIN_BOTTOM + 40) {
                cursorY = addPage(doc, pages, contentStream);
                contentStream = new PDPageContentStream(doc, pages.get(pages.size() - 1),
                        PDPageContentStream.AppendMode.APPEND, true, false);
            }
            Map<String, String> sig = sigs.get(index);
            float signatureX = MARGIN_LEFT + (index % 2) * halfContentWidth;
            String role = sig.getOrDefault("role", "");
            String name = sig.getOrDefault("name", "");
            String city = sig.getOrDefault("city", "");
            String info = String.join("  |  ", role, name, labels.get("cityPrefix") + " " + city);
            List<String> infoLines = wrapText(info, 8.5f, halfContentWidth - 12);
            contentStream.beginText(); contentStream.setFont(FONT, 8.5f); contentStream.setLeading(11);
            contentStream.newLineAtOffset(signatureX, cursorY);
            for (String line : infoLines) { contentStream.showText(line); contentStream.newLine(); }
            contentStream.endText();
            float signatureLineY = cursorY - infoLines.size() * 11 - 4;
            contentStream.setStrokingColor(0.74f, 0.74f, 0.74f); contentStream.setLineWidth(0.5f);
            contentStream.moveTo(signatureX, signatureLineY); contentStream.lineTo(signatureX + halfContentWidth - 20, signatureLineY); contentStream.stroke();
            drawSignatureImage(contentStream, doc, sig.get("signature"), signatureX, signatureLineY, halfContentWidth - 20, 30);
            if (index % 2 == 1) cursorY -= blockHeight;
        }
        contentStream.close();
    }

    private List<String> wrapText(String text, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return List.of("");
        StringBuilder currentLine = new StringBuilder();
        float lineWidth = 0;
        for (String word : text.split(" ")) {
            float wordWidth = textWidth(word + " ", size);
            if (lineWidth + wordWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
                lineWidth = 0;
            }
            currentLine.append(word).append(' ');
            lineWidth += wordWidth;
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString().trim());
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void drawSignatureImage(PDPageContentStream contentStream, PDDocument doc, String dataUrl, float cursorX, float baselineY, float maxWidth, float maxHeight) throws IOException {
        if (dataUrl == null || dataUrl.isBlank() || !dataUrl.startsWith("data:image/")) return;
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) return;
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "signature");
            float scale = Math.min(maxWidth / img.getWidth(), maxHeight / img.getHeight());
            float imageWidth = img.getWidth() * scale;
            float imageHeight = img.getHeight() * scale;
            contentStream.drawImage(img, cursorX + (maxWidth - imageWidth) / 2, baselineY - imageHeight, imageWidth, imageHeight);
        } catch (Exception ignored) {}
    }

    private void drawFooter(PDDocument doc, List<PDPage> pages, PdfLabels labels) throws IOException {
        for (int i = 0; i < pages.size(); i++) {
            PDPageContentStream footerStream = new PDPageContentStream(doc, pages.get(i), PDPageContentStream.AppendMode.APPEND, true);
            String text = labels.get("page") + " " + (i + 1) + "/" + pages.size();
            footerStream.beginText(); footerStream.setFont(FONT, 7); footerStream.newLineAtOffset((PAGE_W - textWidth(text, 7)) / 2, 20); footerStream.showText(text); footerStream.endText();
            footerStream.close();
        }
    }

    private void addPhotoAppendix(PDDocument doc, List<PhotoEntry> photos, List<PDPage> pages, PdfLabels labels) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        pages.add(page);
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        float cursorY = PAGE_H - MARGIN_TOP;
        contentStream.beginText(); contentStream.setFont(FONT_BOLD, 16); contentStream.newLineAtOffset(MARGIN_LEFT, cursorY); contentStream.showText(labels.get("photoIndex")); contentStream.endText();
        cursorY -= 30;

        for (PhotoEntry photo : photos) {
            if (cursorY < 120) {
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                pages.add(page);
                contentStream = new PDPageContentStream(doc, page);
                cursorY = PAGE_H - MARGIN_TOP - 10;
            }
            float imageHeight = 0;
            try {
                Path imgPath = photoStorage.load(photo.storedFileName);
                if (Files.exists(imgPath)) {
                    PDImageXObject img = PDImageXObject.createFromFileByContent(imgPath.toFile(), doc);
                    float scale = Math.min(CONTENT_WIDTH / img.getWidth(), (cursorY - 60) / img.getHeight());
                    float imageWidth = img.getWidth() * scale;
                    imageHeight = img.getHeight() * scale;
                    contentStream.drawImage(img, MARGIN_LEFT + (CONTENT_WIDTH - imageWidth) / 2, cursorY - imageHeight, imageWidth, imageHeight);
                }
            } catch (Exception ignored) {}
            String caption = "(" + photo.id + ") " + photo.section;
            float captionY = cursorY - imageHeight - 14;
            contentStream.beginText(); contentStream.setFont(FONT, 8);
            contentStream.newLineAtOffset(MARGIN_LEFT + (CONTENT_WIDTH - textWidth(caption, 8)) / 2, captionY);
            contentStream.showText(caption);
            contentStream.endText();
            cursorY -= imageHeight + 24;
        }
        contentStream.close();
    }

    private String circled(int index) {
        return "(" + index + ")";
    }

    private float textWidth(String text, float size) throws IOException {
        if (text == null || text.isEmpty()) return 0;
        return FONT.getStringWidth(text) / 1000f * size;
    }

    private float boldTextWidth(String text, float size) throws IOException {
        if (text == null || text.isEmpty()) return 0;
        return FONT_BOLD.getStringWidth(text) / 1000f * size;
    }

    private String nullToEmpty(String text) { return text != null ? text : ""; }

    private Map<String, String> singletonMap(String key, String value) {
        Map<String, String> map = new HashMap<>(); map.put(key, value); return map;
    }

    private static class PhotoEntry {
        int id; String section, detail, storedFileName;
        PhotoEntry(int id, String section, String detail, String storedFileName) { this.id = id; this.section = section; this.detail = detail; this.storedFileName = storedFileName; }
    }
}