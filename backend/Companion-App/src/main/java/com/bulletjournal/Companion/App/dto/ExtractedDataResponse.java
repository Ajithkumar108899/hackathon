package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDataResponse {
    private String title;
    private String type; // task, note, event, habit, emotion
    private String symbol; // •, X, /, O, ⦿, -, etc.
    private String status; // TODO, IN_PROGRESS, COMPLETED, SCHEDULED, etc.
    private String createdDate; // Formatted date string
}

