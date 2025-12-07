package com.bulletjournal.Companion.App.service;

import com.bulletjournal.Companion.App.dto.ExtractedDataResponse;
import com.bulletjournal.Companion.App.dto.JournalEntryRequest;
import com.bulletjournal.Companion.App.dto.JournalEntryResponse;
import com.bulletjournal.Companion.App.model.*;
import com.bulletjournal.Companion.App.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalEntryService {
	
	private final TaskRepository taskRepository;
	private final NoteRepository noteRepository;
	private final EventRepository eventRepository;
	private final EmotionRepository emotionRepository;
	private final UserRepository userRepository;
	private final JournalPageRepository journalPageRepository;
	
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	/**
	 * Get or create a default "Manual Entry" journal page for manual entries
	 */
	@Transactional
	private JournalPage getOrCreateManualJournalPage(User user) {
		// Try to find existing manual journal page
		List<JournalPage> pages = journalPageRepository.findByUserId(user.getId());
		JournalPage manualPage = pages.stream()
			.filter(p -> "MANUAL_ENTRY".equals(p.getThreadId()))
			.findFirst()
			.orElse(null);
		
		if (manualPage == null) {
			// Create a new manual journal page
			manualPage = JournalPage.builder()
				.user(user)
				.imagePath("manual-entry")
				.originalFilename("Manual Entry")
				.extractedText("Manual entries created through API")
				.pageNumber(0)
				.threadId("MANUAL_ENTRY")
				.build();
			manualPage = journalPageRepository.save(manualPage);
		}
		
		return manualPage;
	}
	
	@Transactional
	public JournalEntryResponse createEntry(Long userId, JournalEntryRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("User not found"));
		
		JournalPage manualPage = getOrCreateManualJournalPage(user);
		
		String type = request.getType().toLowerCase();
		
		switch (type) {
			case "task":
				return createTask(user, manualPage, request);
			case "note":
				return createNote(user, manualPage, request);
			case "event":
				return createEvent(user, manualPage, request);
			case "habit":
				// Store habit in emotions table
				return createEmotion(user, manualPage, request);
			case "emotion":
				return createEmotion(user, manualPage, request);
			default:
				throw new IllegalArgumentException("Invalid entry type: " + type);
		}
	}
	
	private JournalEntryResponse createTask(User user, JournalPage journalPage, JournalEntryRequest request) {
		String content = request.getTitle();
		if (request.getNotes() != null && !request.getNotes().isEmpty()) {
			content = request.getTitle() + " - " + request.getNotes();
		}
		
		Task task = Task.builder()
			.user(user)
			.journalPage(journalPage)
			.content(content)
			.status(request.getCompleted() != null && request.getCompleted() 
				? Task.TaskStatus.COMPLETED 
				: Task.TaskStatus.TODO)
			.build();
		
		task = taskRepository.save(task);
		JournalEntryResponse response = taskToResponse(task);
		// Task response always returns "task" type
		return response;
	}
	
	private JournalEntryResponse createNote(User user, JournalPage journalPage, JournalEntryRequest request) {
		String content = request.getTitle();
		if (request.getNotes() != null && !request.getNotes().isEmpty()) {
			content = request.getTitle() + " - " + request.getNotes();
		}
		
		Note.NoteStatus noteStatus = (request.getCompleted() != null && request.getCompleted()) 
			? Note.NoteStatus.COMPLETED 
			: Note.NoteStatus.SCHEDULED;
		
		Note note = Note.builder()
			.user(user)
			.journalPage(journalPage)
			.content(content)
			.status(noteStatus)
			.build();
		
		note = noteRepository.save(note);
		return noteToResponse(note);
	}
	
	private JournalEntryResponse createEvent(User user, JournalPage journalPage, JournalEntryRequest request) {
		String content = request.getTitle();
		if (request.getNotes() != null && !request.getNotes().isEmpty()) {
			content = request.getTitle() + " - " + request.getNotes();
		}
		
		Event event = Event.builder()
			.user(user)
			.journalPage(journalPage)
			.content(content)
			.status(request.getCompleted() != null && request.getCompleted() 
				? Event.EventStatus.COMPLETED 
				: Event.EventStatus.SCHEDULED)
			.build();
		
		// Set event date if provided
		if (request.getDate() != null && !request.getDate().isEmpty()) {
			try {
				event.setEventDate(LocalDate.parse(request.getDate(), DATE_FORMATTER));
			} catch (Exception e) {
				// Ignore invalid date format
			}
		}
		
		event = eventRepository.save(event);
		return eventToResponse(event);
	}
	
	private JournalEntryResponse createEmotion(User user, JournalPage journalPage, JournalEntryRequest request) {
		String content = request.getTitle();
		if (request.getNotes() != null && !request.getNotes().isEmpty()) {
			content = request.getTitle() + " - " + request.getNotes();
		}
		
		// Determine if this is a habit or emotion
		String requestType = request.getType() != null ? request.getType().toLowerCase() : "emotion";
		String emotionTypeValue = null;
		
		if (requestType.equals("habit")) {
			// For habits, set emotionType to "habit"
			emotionTypeValue = "habit";
		} else if (request.getTags() != null && !request.getTags().isEmpty()) {
			// For emotions, use first tag as emotionType
			emotionTypeValue = request.getTags().get(0);
		}
		
		Emotion emotion = Emotion.builder()
			.user(user)
			.journalPage(journalPage)
			.content(content)
			.emotionType(emotionTypeValue)
			.build();
		
		emotion = emotionRepository.save(emotion);
		JournalEntryResponse response = emotionToResponse(emotion);
		// Preserve the original request type (habit or emotion) in response
		response.setType(requestType);
		return response;
	}
	
	@Transactional(readOnly = true)
	public List<JournalEntryResponse> getAllEntries(Long userId) {
		List<JournalEntryResponse> entries = new ArrayList<>();
		
		// Get all tasks
		List<Task> tasks = taskRepository.findByUserId(userId);
		entries.addAll(tasks.stream()
			.map(this::taskToResponse)
			.collect(Collectors.toList()));
		
		// Get all notes
		List<Note> notes = noteRepository.findByUserId(userId);
		entries.addAll(notes.stream()
			.map(this::noteToResponse)
			.collect(Collectors.toList()));
		
		// Get all events
		List<Event> events = eventRepository.findByUserId(userId);
		entries.addAll(events.stream()
			.map(this::eventToResponse)
			.collect(Collectors.toList()));
		
		// Get all emotions
		List<Emotion> emotions = emotionRepository.findByUserId(userId);
		entries.addAll(emotions.stream()
			.map(this::emotionToResponse)
			.collect(Collectors.toList()));
		
		// Sort by createdAt descending
		entries.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
		
		return entries;
	}
	
	@Transactional(readOnly = true)
	public JournalEntryResponse getEntryById(Long userId, String entryId) {
		// Try to parse as Long for database ID
		try {
			Long id = Long.parseLong(entryId);
			
			// Try task first
			Task task = taskRepository.findById(id).orElse(null);
			if (task != null && task.getUser().getId().equals(userId)) {
				return taskToResponse(task);
			}
			
			// Try note
			Note note = noteRepository.findById(id).orElse(null);
			if (note != null && note.getUser().getId().equals(userId)) {
				return noteToResponse(note);
			}
			
			// Try event
			Event event = eventRepository.findById(id).orElse(null);
			if (event != null && event.getUser().getId().equals(userId)) {
				return eventToResponse(event);
			}
		} catch (NumberFormatException e) {
			// Invalid ID format
		}
		
		throw new RuntimeException("Entry not found or access denied");
	}
	
	@Transactional
	public JournalEntryResponse updateEntry(Long userId, String entryId, JournalEntryRequest request) {
		try {
			Long id = Long.parseLong(entryId);
			String newType = request.getType() != null ? request.getType().toLowerCase() : null;
			
			// Determine current entry type
			String currentType = null;
			Object currentEntry = null;
			
			// Try task first
			Task task = taskRepository.findById(id).orElse(null);
			if (task != null && task.getUser().getId().equals(userId)) {
				currentType = "task";
				currentEntry = task;
			}
			
			// Try note
			if (currentEntry == null) {
				Note note = noteRepository.findById(id).orElse(null);
				if (note != null && note.getUser().getId().equals(userId)) {
					currentType = "note";
					currentEntry = note;
				}
			}
			
			// Try event
			if (currentEntry == null) {
				Event event = eventRepository.findById(id).orElse(null);
				if (event != null && event.getUser().getId().equals(userId)) {
					currentType = "event";
					currentEntry = event;
				}
			}
			
			// Try emotion
			if (currentEntry == null) {
				Emotion emotion = emotionRepository.findById(id).orElse(null);
				if (emotion != null && emotion.getUser().getId().equals(userId)) {
					currentType = "emotion";
					currentEntry = emotion;
				}
			}
			
			if (currentEntry == null) {
				throw new RuntimeException("Entry not found or access denied");
			}
			
			// If type is changing, delete from old table and create in new table
			if (newType != null && !newType.equals(currentType)) {
				return changeEntryType(userId, entryId, currentType, newType, request, currentEntry);
			}
			
			// Type not changing, just update in same table
			if (currentType.equals("task")) {
				return updateTask((Task) currentEntry, request);
			} else if (currentType.equals("note")) {
				return updateNote((Note) currentEntry, request);
			} else if (currentType.equals("event")) {
				return updateEvent((Event) currentEntry, request);
			} else if (currentType.equals("emotion")) {
				return updateEmotion((Emotion) currentEntry, request);
			}
			
		} catch (NumberFormatException e) {
			// Invalid ID format
		}
		
		throw new RuntimeException("Entry not found or access denied");
	}
	
	/**
	 * Change entry type by deleting from old table and creating in new table
	 */
	@Transactional
	private JournalEntryResponse changeEntryType(Long userId, String entryId, String oldType, String newType, 
			JournalEntryRequest request, Object oldEntry) {
		Long id = Long.parseLong(entryId);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("User not found"));
		JournalPage manualPage = getOrCreateManualJournalPage(user);
		
		// Extract data from old entry
		String content = null;
		LocalDate eventDate = null;
		Boolean completed = false;
		
		if (oldEntry instanceof Task) {
			Task task = (Task) oldEntry;
			content = task.getContent();
			completed = task.getStatus() == Task.TaskStatus.COMPLETED;
		} else if (oldEntry instanceof Note) {
			Note note = (Note) oldEntry;
			content = note.getContent();
			completed = note.getStatus() == Note.NoteStatus.COMPLETED;
		} else if (oldEntry instanceof Event) {
			Event event = (Event) oldEntry;
			content = event.getContent();
			eventDate = event.getEventDate();
			completed = event.getStatus() == Event.EventStatus.COMPLETED;
		} else if (oldEntry instanceof Emotion) {
			Emotion emotion = (Emotion) oldEntry;
			content = emotion.getContent();
		}
		
		// Use request data if provided, otherwise use old entry data
		String title = request.getTitle() != null ? request.getTitle() : content;
		String notes = request.getNotes() != null ? request.getNotes() : null;
		if (title == null || title.trim().isEmpty()) {
			title = "Untitled Entry";
		}
		
		// Delete from old table
		if (oldType.equals("task")) {
			taskRepository.delete((Task) oldEntry);
		} else if (oldType.equals("note")) {
			noteRepository.delete((Note) oldEntry);
		} else if (oldType.equals("event")) {
			eventRepository.delete((Event) oldEntry);
		} else if (oldType.equals("emotion")) {
			emotionRepository.delete((Emotion) oldEntry);
		}
		
		// Create in new table
		JournalEntryRequest createRequest = new JournalEntryRequest();
		// Ensure the type is set correctly (don't let habit convert to task)
		createRequest.setType(newType); // String type - preserve the requested type
		createRequest.setTitle(title);
		createRequest.setNotes(notes);
		createRequest.setCompleted(request.getCompleted() != null ? request.getCompleted() : completed);
		if (request.getDate() != null && !request.getDate().isEmpty()) {
			createRequest.setDate(request.getDate());
		} else if (eventDate != null) {
			createRequest.setDate(eventDate.format(DATE_FORMATTER));
		}
		createRequest.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
		
		// Create entry with the new type
		JournalEntryResponse response = createEntry(userId, createRequest);
		
		// Ensure the response type matches what was requested
		// Since "habit" is now stored in emotions table, it should return "habit" type
		if (response != null && newType != null) {
			// Preserve the requested type in response
			if (newType.equals("habit") || newType.equals("emotion")) {
				// Both are stored in emotions table, but preserve the requested type
				response.setType(newType);
			} else if (!newType.equals(response.getType())) {
				// For other types, ensure they match
				response.setType(newType);
			}
		}
		
		return response;
	}
	
	private JournalEntryResponse updateTask(Task task, JournalEntryRequest request) {
		if (request.getTitle() != null) {
			String content = request.getTitle();
			if (request.getNotes() != null && !request.getNotes().isEmpty()) {
				content += " - " + request.getNotes();
			}
			task.setContent(content);
		}
		
		if (request.getCompleted() != null) {
			task.setStatus(request.getCompleted() 
				? Task.TaskStatus.COMPLETED 
				: Task.TaskStatus.TODO);
		}
		
		task = taskRepository.save(task);
		return taskToResponse(task);
	}
	
	private JournalEntryResponse updateNote(Note note, JournalEntryRequest request) {
		if (request.getTitle() != null) {
			String content = request.getTitle();
			if (request.getNotes() != null && !request.getNotes().isEmpty()) {
				content += " - " + request.getNotes();
			}
			note.setContent(content);
		}
		
		if (request.getCompleted() != null) {
			note.setStatus(request.getCompleted() 
				? Note.NoteStatus.COMPLETED 
				: Note.NoteStatus.SCHEDULED);
		}
		
		note = noteRepository.save(note);
		return noteToResponse(note);
	}
	
	private JournalEntryResponse updateEvent(Event event, JournalEntryRequest request) {
		if (request.getTitle() != null) {
			String content = request.getTitle();
			if (request.getNotes() != null && !request.getNotes().isEmpty()) {
				content += " - " + request.getNotes();
			}
			event.setContent(content);
		}
		
		if (request.getCompleted() != null) {
			event.setStatus(request.getCompleted() 
				? Event.EventStatus.COMPLETED 
				: Event.EventStatus.SCHEDULED);
		}
		
		if (request.getDate() != null && !request.getDate().isEmpty()) {
			try {
				event.setEventDate(LocalDate.parse(request.getDate(), DATE_FORMATTER));
			} catch (Exception e) {
				// Ignore invalid date format
			}
		}
		
		event = eventRepository.save(event);
		return eventToResponse(event);
	}
	
	private JournalEntryResponse updateEmotion(Emotion emotion, JournalEntryRequest request) {
		if (request.getTitle() != null) {
			String content = request.getTitle();
			if (request.getNotes() != null && !request.getNotes().isEmpty()) {
				content += " - " + request.getNotes();
			}
			emotion.setContent(content);
		}
		
		if (request.getTags() != null && !request.getTags().isEmpty()) {
			emotion.setEmotionType(request.getTags().get(0));
		}
		
		emotion = emotionRepository.save(emotion);
		return emotionToResponse(emotion);
	}
	
	@Transactional
	public void deleteEntry(Long userId, String entryId) {
		try {
			Long id = Long.parseLong(entryId);
			
			// Check all tables to find and delete the entry
			// Try task first
			Task task = taskRepository.findById(id).orElse(null);
			if (task != null && task.getUser().getId().equals(userId)) {
				taskRepository.delete(task);
				return;
			}
			
			// Try note
			Note note = noteRepository.findById(id).orElse(null);
			if (note != null && note.getUser().getId().equals(userId)) {
				noteRepository.delete(note);
				return;
			}
			
			// Try event
			Event event = eventRepository.findById(id).orElse(null);
			if (event != null && event.getUser().getId().equals(userId)) {
				eventRepository.delete(event);
				return;
			}
			
			// Try emotion (includes habits stored in emotions table)
			Emotion emotion = emotionRepository.findById(id).orElse(null);
			if (emotion != null && emotion.getUser().getId().equals(userId)) {
				emotionRepository.delete(emotion);
				return;
			}
			
			// If we reach here, entry was not found in any table
			throw new RuntimeException("Entry not found in any table (tasks, notes, events, emotions) for ID: " + entryId);
			
		} catch (NumberFormatException e) {
			throw new RuntimeException("Invalid entry ID format: " + entryId);
		}
	}
	
	@Transactional
	public JournalEntryResponse toggleComplete(Long userId, String entryId, Boolean completed) {
		try {
			Long id = Long.parseLong(entryId);
			
			// Try task first
			Task task = taskRepository.findById(id).orElse(null);
			if (task != null && task.getUser().getId().equals(userId)) {
				task.setStatus(completed != null && completed 
					? Task.TaskStatus.COMPLETED 
					: Task.TaskStatus.TODO);
				task = taskRepository.save(task);
				return taskToResponse(task);
			}
			
			// Try note
			Note note = noteRepository.findById(id).orElse(null);
			if (note != null && note.getUser().getId().equals(userId)) {
				if (completed != null) {
					note.setStatus(completed ? Note.NoteStatus.COMPLETED : Note.NoteStatus.SCHEDULED);
				} else {
					// Toggle: if currently COMPLETED, set to SCHEDULED, otherwise set to COMPLETED
					note.setStatus(note.getStatus() == Note.NoteStatus.COMPLETED 
						? Note.NoteStatus.SCHEDULED 
						: Note.NoteStatus.COMPLETED);
				}
				note = noteRepository.save(note);
				return noteToResponse(note);
			}
			
			// Try event
			Event event = eventRepository.findById(id).orElse(null);
			if (event != null && event.getUser().getId().equals(userId)) {
				event.setStatus(completed != null && completed 
					? Event.EventStatus.COMPLETED 
					: Event.EventStatus.SCHEDULED);
				event = eventRepository.save(event);
				return eventToResponse(event);
			}
		} catch (NumberFormatException e) {
			// Invalid ID format
		}
		
		throw new RuntimeException("Entry not found or access denied");
	}
	
	// Conversion methods
	private JournalEntryResponse taskToResponse(Task task) {
		String content = task.getContent();
		String title = content;
		String notes = null;
		
		// Handle null or empty content
		if (content == null || content.trim().isEmpty() || content.trim().equals("_")) {
			title = "Untitled Task";
			notes = null;
		} else {
			// Try to split title and notes if they're combined
			if (content.contains(" - ")) {
				String[] parts = content.split(" - ", 2);
				title = parts[0].trim();
				notes = parts.length > 1 ? parts[1].trim() : null;
			} else {
				title = content.trim();
				notes = null;
			}
			
			// If title is too short or just a single character, use a default
			if (title.length() <= 1) {
				title = "Task " + task.getId();
			}
		}
		
		return JournalEntryResponse.builder()
			.id(String.valueOf(task.getId()))
			.type("task")
			.title(title)
			.notes(notes)
			.completed(task.getStatus() == Task.TaskStatus.COMPLETED)
			.date(null) // Tasks don't have dates in current model
			.createdAt(formatDateTime(task.getCreatedAt()))
			.updatedAt(formatDateTime(task.getUpdatedAt()))
			.tags(new ArrayList<>()) // Tags not in current model
			.build();
	}
	
	private JournalEntryResponse noteToResponse(Note note) {
		String content = note.getContent();
		String title = content;
		String notes = null;
		
		// Try to split title and notes if they're combined
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0];
			notes = parts.length > 1 ? parts[1] : null;
		}
		
		return JournalEntryResponse.builder()
			.id(String.valueOf(note.getId()))
			.type("note")
			.title(title)
			.notes(notes)
			.completed(note.getStatus() == Note.NoteStatus.COMPLETED)
			.date(null)
			.createdAt(formatDateTime(note.getCreatedAt()))
			.updatedAt(formatDateTime(note.getUpdatedAt()))
			.tags(new ArrayList<>()) // Tags not in current model
			.build();
	}
	
	private JournalEntryResponse eventToResponse(Event event) {
		String content = event.getContent();
		String title = content;
		String notes = null;
		
		// Try to split title and notes if they're combined
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0];
			notes = parts.length > 1 ? parts[1] : null;
		}
		
		return JournalEntryResponse.builder()
			.id(String.valueOf(event.getId()))
			.type("event")
			.title(title)
			.notes(notes)
			.completed(event.getStatus() == Event.EventStatus.COMPLETED)
			.date(event.getEventDate() != null ? event.getEventDate().format(DATE_FORMATTER) : null)
			.createdAt(formatDateTime(event.getCreatedAt()))
			.updatedAt(formatDateTime(event.getUpdatedAt()))
			.tags(new ArrayList<>()) // Tags not in current model
			.build();
	}
	
	private JournalEntryResponse emotionToResponse(Emotion emotion) {
		String content = emotion.getContent();
		String title = content;
		String notes = null;
		
		// Handle null or empty content
		if (content == null || content.trim().isEmpty() || content.trim().equals("_")) {
			title = "Untitled Entry";
			notes = null;
		} else {
			// Try to split title and notes if they're combined
			if (content.contains(" - ")) {
				String[] parts = content.split(" - ", 2);
				title = parts[0].trim();
				notes = parts.length > 1 ? parts[1].trim() : null;
			} else {
				title = content.trim();
				notes = null;
			}
			
			// If title is too short or just a single character, use a default
			if (title.length() <= 1) {
				// Check if it's a habit based on emotionType
				if (emotion.getEmotionType() != null && emotion.getEmotionType().equals("habit")) {
					title = "Habit " + emotion.getId();
				} else {
					title = "Emotion " + emotion.getId();
				}
			}
		}
		
		// Determine type based on emotionType field
		String entryType = "emotion"; // Default
		if (emotion.getEmotionType() != null && emotion.getEmotionType().equals("habit")) {
			entryType = "habit";
		}
		
		List<String> tags = new ArrayList<>();
		if (emotion.getEmotionType() != null && !emotion.getEmotionType().trim().isEmpty() 
				&& !emotion.getEmotionType().equals("habit")) {
			// Only add to tags if it's not "habit" (habit is stored in emotionType but not as a tag)
			tags.add(emotion.getEmotionType());
		}
		
		return JournalEntryResponse.builder()
			.id(String.valueOf(emotion.getId()))
			.type(entryType) // "habit" or "emotion" based on emotionType field
			.title(title)
			.notes(notes)
			.completed(false) // Emotions and habits are never completed
			.date(null)
			.createdAt(formatDateTime(emotion.getCreatedAt()))
			.updatedAt(formatDateTime(emotion.getUpdatedAt()))
			.tags(tags)
			.build();
	}
	
	private String formatDateTime(LocalDateTime dateTime) {
		if (dateTime == null) return null;
		return dateTime.format(DATETIME_FORMATTER);
	}
	
	/**
	 * Get extracted data from all tables (tasks, notes, events, emotions) for the user
	 * Returns ONLY data from scanned images (excludes manual entries)
	 * If journalPageId is provided, returns only entries from that specific scan
	 * Returns data in format suitable for Extracted Data View page
	 */
	@Transactional(readOnly = true)
	public List<ExtractedDataResponse> getExtractedData(Long userId, Long journalPageId) {
		List<ExtractedDataResponse> extractedData = new ArrayList<>();
		
		// If journalPageId is provided, get entries only from that specific scan
		if (journalPageId != null) {
			// Get tasks from specific journal page
			List<Task> tasks = taskRepository.findByJournalPageId(journalPageId);
			extractedData.addAll(tasks.stream()
				.filter(task -> task.getUser().getId().equals(userId)) // Verify user ownership
				.map(this::taskToExtractedData)
				.collect(Collectors.toList()));
			
			// Get notes from specific journal page
			List<Note> notes = noteRepository.findByJournalPageId(journalPageId);
			extractedData.addAll(notes.stream()
				.filter(note -> note.getUser().getId().equals(userId))
				.map(this::noteToExtractedData)
				.collect(Collectors.toList()));
			
			// Get events from specific journal page
			List<Event> events = eventRepository.findByJournalPageId(journalPageId);
			extractedData.addAll(events.stream()
				.filter(event -> event.getUser().getId().equals(userId))
				.map(this::eventToExtractedData)
				.collect(Collectors.toList()));
			
			// Get emotions from specific journal page
			List<Emotion> emotions = emotionRepository.findByJournalPageId(journalPageId);
			extractedData.addAll(emotions.stream()
				.filter(emotion -> emotion.getUser().getId().equals(userId))
				.map(this::emotionToExtractedData)
				.collect(Collectors.toList()));
		} else {
			// Get all tasks - filter out manual entries
			List<Task> tasks = taskRepository.findByUserId(userId);
			extractedData.addAll(tasks.stream()
				.filter(task -> {
					try {
						// Access journalPage to trigger lazy loading
						JournalPage page = task.getJournalPage();
						return !isManualEntry(page);
					} catch (Exception e) {
						// If lazy loading fails, assume it's not manual (safer to include)
						return true;
					}
				})
				.map(this::taskToExtractedData)
				.collect(Collectors.toList()));
			
			// Get all notes - filter out manual entries
			List<Note> notes = noteRepository.findByUserId(userId);
			extractedData.addAll(notes.stream()
				.filter(note -> {
					try {
						JournalPage page = note.getJournalPage();
						return !isManualEntry(page);
					} catch (Exception e) {
						return true;
					}
				})
				.map(this::noteToExtractedData)
				.collect(Collectors.toList()));
			
			// Get all events - filter out manual entries
			List<Event> events = eventRepository.findByUserId(userId);
			extractedData.addAll(events.stream()
				.filter(event -> {
					try {
						JournalPage page = event.getJournalPage();
						return !isManualEntry(page);
					} catch (Exception e) {
						return true;
					}
				})
				.map(this::eventToExtractedData)
				.collect(Collectors.toList()));
			
			// Get all emotions (includes habits) - filter out manual entries
			List<Emotion> emotions = emotionRepository.findByUserId(userId);
			extractedData.addAll(emotions.stream()
				.filter(emotion -> {
					try {
						JournalPage page = emotion.getJournalPage();
						return !isManualEntry(page);
					} catch (Exception e) {
						return true;
					}
				})
				.map(this::emotionToExtractedData)
				.collect(Collectors.toList()));
		}
		
		// Sort by createdAt descending (newest first)
		extractedData.sort((a, b) -> {
			try {
				LocalDateTime dateA = LocalDateTime.parse(a.getCreatedDate(), DATETIME_FORMATTER);
				LocalDateTime dateB = LocalDateTime.parse(b.getCreatedDate(), DATETIME_FORMATTER);
				return dateB.compareTo(dateA);
			} catch (Exception e) {
				return 0;
			}
		});
		
		return extractedData;
	}
	
	/**
	 * Check if a journal page is a manual entry (not from scanned image)
	 */
	private boolean isManualEntry(JournalPage journalPage) {
		if (journalPage == null) {
			return false; // If no journal page, consider it as scanned (edge case)
		}
		
		// Manual entries have threadId "MANUAL_ENTRY" or originalFilename "Manual Entry"
		String threadId = journalPage.getThreadId();
		String originalFilename = journalPage.getOriginalFilename();
		String imagePath = journalPage.getImagePath();
		
		return ("MANUAL_ENTRY".equals(threadId) || 
				"Manual Entry".equals(originalFilename) ||
				"manual-entry".equals(imagePath));
	}
	
	private ExtractedDataResponse taskToExtractedData(Task task) {
		String content = task.getContent();
		String title = content;
		
		// Extract title from content
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0].trim();
		} else if (content != null) {
			title = content.trim();
		}
		
		if (title == null || title.trim().isEmpty() || title.trim().equals("_")) {
			title = "Untitled Task";
		}
		
		// Determine symbol based on status
		String symbol = task.getSymbol() != null ? task.getSymbol() : "•";
		if (task.getStatus() == Task.TaskStatus.COMPLETED) {
			symbol = "X";
		} else if (task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
			symbol = "/";
		} else {
			symbol = "•";
		}
		
		return ExtractedDataResponse.builder()
			.title(title)
			.type("task")
			.symbol(symbol)
			.status(task.getStatus().name())
			.createdDate(formatDateTime(task.getCreatedAt()))
			.build();
	}
	
	private ExtractedDataResponse noteToExtractedData(Note note) {
		String content = note.getContent();
		String title = content;
		
		// Extract title from content
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0].trim();
		} else if (content != null) {
			title = content.trim();
		}
		
		if (title == null || title.trim().isEmpty() || title.trim().equals("_")) {
			title = "Untitled Note";
		}
		
		// Determine symbol based on status
		String symbol = "-";
		if (note.getStatus() == Note.NoteStatus.COMPLETED) {
			symbol = "⦿";
		} else {
			symbol = "-";
		}
		
		return ExtractedDataResponse.builder()
			.title(title)
			.type("note")
			.symbol(symbol)
			.status(note.getStatus().name())
			.createdDate(formatDateTime(note.getCreatedAt()))
			.build();
	}
	
	private ExtractedDataResponse eventToExtractedData(Event event) {
		String content = event.getContent();
		String title = content;
		
		// Extract title from content
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0].trim();
		} else if (content != null) {
			title = content.trim();
		}
		
		if (title == null || title.trim().isEmpty() || title.trim().equals("_")) {
			title = "Untitled Event";
		}
		
		// Determine symbol based on status
		String symbol = event.getSymbol() != null ? event.getSymbol() : "O";
		if (event.getStatus() == Event.EventStatus.COMPLETED) {
			symbol = "⦿";
		} else {
			symbol = "O";
		}
		
		// Determine status display
		String statusDisplay = event.getStatus().name();
		if (event.getEventDate() != null && event.getEventDate().isAfter(LocalDate.now())) {
			statusDisplay = "Upcoming";
		} else if (event.getStatus() == Event.EventStatus.COMPLETED) {
			statusDisplay = "Completed";
		} else {
			statusDisplay = "Scheduled";
		}
		
		return ExtractedDataResponse.builder()
			.title(title)
			.type("event")
			.symbol(symbol)
			.status(statusDisplay)
			.createdDate(formatDateTime(event.getCreatedAt()))
			.build();
	}
	
	private ExtractedDataResponse emotionToExtractedData(Emotion emotion) {
		String content = emotion.getContent();
		String title = content;
		
		// Extract title from content
		if (content != null && content.contains(" - ")) {
			String[] parts = content.split(" - ", 2);
			title = parts[0].trim();
		} else if (content != null) {
			title = content.trim();
		}
		
		if (title == null || title.trim().isEmpty() || title.trim().equals("_")) {
			title = "Untitled Emotion";
		}
		
		// Determine type (habit or emotion)
		String entryType = "emotion";
		if (emotion.getEmotionType() != null && emotion.getEmotionType().equals("habit")) {
			entryType = "habit";
		}
		
		// Determine symbol based on emotion type
		String symbol = "😊"; // Default emotion symbol
		if (emotion.getEmotionType() != null) {
			switch (emotion.getEmotionType().toLowerCase()) {
				case "happy":
				case "joy":
					symbol = "😊";
					break;
				case "sad":
					symbol = "😢";
					break;
				case "anxious":
				case "anxiety":
					symbol = "😰";
					break;
				case "grateful":
					symbol = "🙏";
					break;
				case "habit":
					symbol = "🔄";
					break;
				default:
					symbol = "😊";
			}
		}
		
		return ExtractedDataResponse.builder()
			.title(title)
			.type(entryType)
			.symbol(symbol)
			.status(emotion.getStatus() != null ? emotion.getStatus().name() : "SCHEDULED")
			.createdDate(formatDateTime(emotion.getCreatedAt()))
			.build();
	}
}

