package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
	
	private List<TaskResponse> tasks;
	private List<EventResponse> events;
	private List<NoteResponse> notes;
	private List<EmotionResponse> emotions;
	private int totalResults;
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TaskResponse {
		private Long id;
		private String content;
		private String status;
		private String symbol;
		private Integer pageNumber;
		private Long journalPageId;
	}
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EventResponse {
		private Long id;
		private String content;
		private String status;
		private String eventDate;
		private String symbol;
		private Integer pageNumber;
		private Long journalPageId;
	}
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NoteResponse {
		private Long id;
		private String content;
		private Integer pageNumber;
		private Long journalPageId;
	}
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EmotionResponse {
		private Long id;
		private String content;
		private String emotionType;
		private Integer pageNumber;
		private Long journalPageId;
	}
}

