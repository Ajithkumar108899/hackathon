package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResponse {
	
	private String id;
	
	private String type; // "task", "note", "event", "habit"
	
	private String title;
	
	private String notes;
	
	private Boolean completed;
	
	private String date; // ISO date format: YYYY-MM-DD
	
	private String createdAt; // ISO datetime
	
	private String updatedAt; // ISO datetime
	
	private List<String> tags;
}

