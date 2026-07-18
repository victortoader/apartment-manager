package com.apartmentmanager.controller;

import java.time.LocalDateTime;
import java.util.List;

public record ApartmentSummaryDto(
    Long id,
    String title,
    String location,
    Double price,
    Integer rooms,
    Double area,
    String tenant,
    List<String> photoPaths,
    List<BillSummary> recentBills,
    int openTickets
) {
    public record BillSummary(
        Long id,
        String originalFileName,
        String storedFileName,
        String billType,
        LocalDateTime uploadDate
    ) {}
}
