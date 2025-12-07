package com.bulletjournal.Companion.App.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequest {
	
	@NotNull(message = "Entry type is required")
	@NotBlank(message = "Entry type cannot be blank")
	private String type; // "task", "note", "event", "habit"
	
	@NotNull(message = "Title is required")
	@NotBlank(message = "Title cannot be blank")
	@Size(min = 2, message = "Title must be at least 2 characters")
	private String title;
	
	private String notes;
	
	private Boolean completed;
	
	private String date; // ISO date format: YYYY-MM-DD
	
	private List<String> tags;
}

