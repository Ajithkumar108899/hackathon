package com.bulletjournal.Companion.App.service;

import com.bulletjournal.Companion.App.dto.SearchRequest;
import com.bulletjournal.Companion.App.dto.SearchResponse;
import com.bulletjournal.Companion.App.model.*;
import com.bulletjournal.Companion.App.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

	private final TaskRepository taskRepository;
	private final EventRepository eventRepository;
	private final NoteRepository noteRepository;
	private final EmotionRepository emotionRepository;

	/**
	 * Search across all content types
	 */
	public SearchResponse search(Long userId, SearchRequest request) {
		String query = request.getQuery() != null ? request.getQuery().trim() : "";
		String type = request.getType() != null ? request.getType().toLowerCase() : "all";
		String status = request.getStatus() != null ? request.getStatus().toUpperCase() : null;

		SearchResponse.SearchResponseBuilder responseBuilder = SearchResponse.builder();

		List<SearchResponse.TaskResponse> tasks = new ArrayList<>();
		List<SearchResponse.EventResponse> events = new ArrayList<>();
		List<SearchResponse.NoteResponse> notes = new ArrayList<>();
		List<SearchResponse.EmotionResponse> emotions = new ArrayList<>();

		// Search tasks
		if (type.equals("all") || type.equals("task")) {
			List<Task> taskResults;
			if (query.isEmpty()) {
				taskResults = taskRepository.findByUserId(userId);
			} else {
				taskResults = taskRepository.searchByContent(userId, query);
			}

			// Filter by status if provided
			if (status != null) {
				try {
					Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status);
					taskResults = taskResults.stream()
							.filter(t -> t.getStatus() == taskStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					log.warn("Invalid task status filter: {}", status);
				}
			}

			tasks = taskResults.stream()
					.map(task -> SearchResponse.TaskResponse.builder()
							.id(task.getId())
							.content(task.getContent())
							.status(task.getStatus().name())
							.symbol(task.getSymbol())
							.pageNumber(task.getJournalPage() != null ? task.getJournalPage().getPageNumber() : null)
							.journalPageId(task.getJournalPage() != null ? task.getJournalPage().getId() : null)
							.build())
					.collect(Collectors.toList());
		}

		// Search events
		if (type.equals("all") || type.equals("event")) {
			List<Event> eventResults;
			if (query.isEmpty()) {
				eventResults = eventRepository.findByUserId(userId);
			} else {
				eventResults = eventRepository.searchByContent(userId, query);
			}

			// Filter by status if provided
			if (status != null) {
				try {
					Event.EventStatus eventStatus = Event.EventStatus.valueOf(status);
					eventResults = eventResults.stream()
							.filter(e -> e.getStatus() == eventStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					log.warn("Invalid event status filter: {}", status);
				}
			}

			events = eventResults.stream()
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
		}

		// Search notes
		if (type.equals("all") || type.equals("note")) {
			List<Note> noteResults;
			if (query.isEmpty()) {
				noteResults = noteRepository.findByUserId(userId);
			} else {
				noteResults = noteRepository.searchByContent(userId, query);
			}

			notes = noteResults.stream()
					.map(note -> SearchResponse.NoteResponse.builder()
							.id(note.getId())
							.content(note.getContent())
							.pageNumber(note.getJournalPage() != null ? note.getJournalPage().getPageNumber() : null)
							.journalPageId(note.getJournalPage() != null ? note.getJournalPage().getId() : null)
							.build())
					.collect(Collectors.toList());
		}

		// Search emotions
		if (type.equals("all") || type.equals("emotion")) {
			List<Emotion> emotionResults;
			if (query.isEmpty()) {
				emotionResults = emotionRepository.findByUserId(userId);
			} else {
				emotionResults = emotionRepository.searchByContent(userId, query);
			}

			emotions = emotionResults.stream()
					.map(emotion -> SearchResponse.EmotionResponse.builder()
							.id(emotion.getId())
							.content(emotion.getContent())
							.emotionType(emotion.getEmotionType())
							.pageNumber(emotion.getJournalPage() != null ? emotion.getJournalPage().getPageNumber() : null)
							.journalPageId(emotion.getJournalPage() != null ? emotion.getJournalPage().getId() : null)
							.build())
					.collect(Collectors.toList());
		}

		int totalResults = tasks.size() + events.size() + notes.size() + emotions.size();

		return responseBuilder
				.tasks(tasks)
				.events(events)
				.notes(notes)
				.emotions(emotions)
				.totalResults(totalResults)
				.build();
	}
}

