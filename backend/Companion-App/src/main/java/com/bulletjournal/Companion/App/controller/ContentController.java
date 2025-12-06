package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.dto.SearchResponse;
import com.bulletjournal.Companion.App.model.*;
import com.bulletjournal.Companion.App.repository.*;
import com.bulletjournal.Companion.App.service.ContentExtractionService;
import com.bulletjournal.Companion.App.service.JournalPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journal/content")
@RequiredArgsConstructor
@Tag(name = "Content Management", description = "APIs for retrieving tasks, events, notes, and emotions")
@SecurityRequirement(name = "Bearer Authentication")
public class ContentController {

	private final TaskRepository taskRepository;
	private final EventRepository eventRepository;
	private final NoteRepository noteRepository;
	private final EmotionRepository emotionRepository;
	private final JournalPageService journalPageService;
	private final ContentExtractionService contentExtractionService;

	@GetMapping("/tasks")
	@Operation(summary = "Get all tasks", description = "Retrieve all tasks for the current user")
	public ResponseEntity<List<SearchResponse.TaskResponse>> getAllTasks(@AuthenticationPrincipal User user) {
		List<Task> tasks = taskRepository.findByUserId(user.getId());
		List<SearchResponse.TaskResponse> response = tasks.stream()
				.map(task -> SearchResponse.TaskResponse.builder()
						.id(task.getId())
						.content(task.getContent())
						.status(task.getStatus().name())
						.symbol(task.getSymbol())
						.pageNumber(task.getJournalPage() != null ? task.getJournalPage().getPageNumber() : null)
						.journalPageId(task.getJournalPage() != null ? task.getJournalPage().getId() : null)
						.build())
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/events")
	@Operation(summary = "Get all events", description = "Retrieve all events for the current user")
	public ResponseEntity<List<SearchResponse.EventResponse>> getAllEvents(@AuthenticationPrincipal User user) {
		List<Event> events = eventRepository.findByUserId(user.getId());
		List<SearchResponse.EventResponse> response = events.stream()
				.map(event -> SearchResponse.EventResponse.builder()
						.id(event.getId())
						.content(event.getContent())
						.status(event.getStatus().name())
						.eventDate(event.getEventDate() != null ? 
								event.getEventDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
						.symbol(event.getSymbol())
						.pageNumber(event.getJournalPage() != null ? event.getJournalPage().getPageNumber() : null)
						.journalPageId(event.getJournalPage() != null ? event.getJournalPage().getId() : null)
						.build())
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/notes")
	@Operation(summary = "Get all notes", description = "Retrieve all notes for the current user")
	public ResponseEntity<List<SearchResponse.NoteResponse>> getAllNotes(@AuthenticationPrincipal User user) {
		List<Note> notes = noteRepository.findByUserId(user.getId());
		List<SearchResponse.NoteResponse> response = notes.stream()
				.map(note -> SearchResponse.NoteResponse.builder()
						.id(note.getId())
						.content(note.getContent())
						.pageNumber(note.getJournalPage() != null ? note.getJournalPage().getPageNumber() : null)
						.journalPageId(note.getJournalPage() != null ? note.getJournalPage().getId() : null)
						.build())
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/emotions")
	@Operation(summary = "Get all emotions", description = "Retrieve all emotions for the current user")
	public ResponseEntity<List<SearchResponse.EmotionResponse>> getAllEmotions(@AuthenticationPrincipal User user) {
		List<Emotion> emotions = emotionRepository.findByUserId(user.getId());
		List<SearchResponse.EmotionResponse> response = emotions.stream()
				.map(emotion -> SearchResponse.EmotionResponse.builder()
						.id(emotion.getId())
						.content(emotion.getContent())
						.emotionType(emotion.getEmotionType())
						.pageNumber(emotion.getJournalPage() != null ? emotion.getJournalPage().getPageNumber() : null)
						.journalPageId(emotion.getJournalPage() != null ? emotion.getJournalPage().getId() : null)
						.build())
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/pages/{pageId}/re-extract")
	@Operation(summary = "Re-extract content from page", 
			   description = "Re-run content extraction on a previously scanned page (useful for updates)")
	public ResponseEntity<String> reExtractContent(
			@AuthenticationPrincipal User user,
			@PathVariable Long pageId) {
		try {
			// Get the journal page
			com.bulletjournal.Companion.App.model.JournalPage journalPage = 
					journalPageService.getJournalPageById(pageId, user.getId());
			
			if (journalPage.getExtractedText() == null || journalPage.getExtractedText().isEmpty()) {
				return ResponseEntity.badRequest()
						.body("No extracted text found for this page. Please scan the page first.");
			}

			// Re-extract content
			ContentExtractionService.ExtractionResult result = 
					contentExtractionService.extractAndSaveContent(
							journalPage.getExtractedText(), journalPage, user);

			return ResponseEntity.ok(String.format(
					"Content re-extracted successfully. Found: %d tasks, %d events, %d notes, %d emotions",
					result.getTasksCount(), result.getEventsCount(), 
					result.getNotesCount(), result.getEmotionsCount()));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}

