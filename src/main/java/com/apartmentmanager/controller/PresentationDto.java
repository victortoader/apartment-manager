package com.apartmentmanager.controller;

import java.util.List;

public record PresentationDto(
    Long id,
    String title,
    String location,
    Double price,
    Integer rooms,
    Double area,
    String description,
    String presentation,
    List<String> photoPaths
) {}
