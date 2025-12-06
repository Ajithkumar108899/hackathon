package com.bulletjournal.Companion.App.dto;

import lombok.Data;

@Data
public class SearchRequest {
	
	private String query; // Search keyword
	private String type; // Optional: "task", "event", "note", "emotion", or "all"
	private String status; // Optional: for tasks/events - filter by status
}

