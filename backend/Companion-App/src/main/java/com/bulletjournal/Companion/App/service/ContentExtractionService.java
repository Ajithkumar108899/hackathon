package com.bulletjournal.Companion.App.service;

import com.bulletjournal.Companion.App.model.*;
import com.bulletjournal.Companion.App.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentExtractionService {

	private final TaskRepository taskRepository;
	private final EventRepository eventRepository;
	private final NoteRepository noteRepository;
	private final EmotionRepository emotionRepository;

	/**
	 * Parse extracted text and create Tasks, Events, Notes, Emotions
	 */
	@Transactional
	public ExtractionResult extractAndSaveContent(String extractedText, JournalPage journalPage, User user) {
		if (extractedText == null || extractedText.trim().isEmpty() || extractedText.startsWith("OCR extraction failed")) {
			log.warn("No valid text to extract for journal page: {}", journalPage.getId());
			return new ExtractionResult(0, 0, 0, 0);
		}

		List<Task> tasks = new ArrayList<>();
		List<Event> events = new ArrayList<>();
		List<Note> notes = new ArrayList<>();
		List<Emotion> emotions = new ArrayList<>();

		// Split text into lines
		String[] lines = extractedText.split("\n");
		
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				continue;
			}

			int lineNumber = i + 1;
			String positionHash = generatePositionHash(line, lineNumber);

			// Detect and parse based on symbols (priority order: task > event > emotion > note)
			if (isTask(line)) {
				Task task = parseTask(line, lineNumber, positionHash, journalPage, user);
				if (task != null) {
					tasks.add(task);
				}
			} else if (isEvent(line)) {
				Event event = parseEvent(line, lineNumber, positionHash, journalPage, user);
				if (event != null) {
					events.add(event);
				}
			} else if (isEmotion(line)) {
				Emotion emotion = parseEmotion(line, lineNumber, positionHash, journalPage, user);
				if (emotion != null) {
					emotions.add(emotion);
				}
			} else if (isNote(line)) {
				// Explicit note pattern (starts with -)
				Note note = parseNote(line, lineNumber, positionHash, journalPage, user);
				if (note != null) {
					notes.add(note);
				}
			} else {
				// Default to note if no symbol detected (but only if line is meaningful)
				if (line.length() > 3) { // Ignore very short lines
					Note note = parseNote(line, lineNumber, positionHash, journalPage, user);
					if (note != null) {
						notes.add(note);
					}
				}
			}
		}

		// Save all extracted items (with duplicate detection)
		int tasksSaved = saveTasks(tasks, journalPage);
		int eventsSaved = saveEvents(events, journalPage);
		int notesSaved = saveNotes(notes, journalPage);
		int emotionsSaved = saveEmotions(emotions, journalPage);

		log.info("Extraction completed for page {}: {} tasks, {} events, {} notes, {} emotions",
				journalPage.getId(), tasksSaved, eventsSaved, notesSaved, emotionsSaved);

		return new ExtractionResult(tasksSaved, eventsSaved, notesSaved, emotionsSaved);
	}

	// Improved patterns for detecting bullet journal symbols (with MULTILINE support)
	private static final Pattern TASK_PATTERN = Pattern.compile("^[\\s]*([•·\\-]|X|/)[\\s]*(.+)$", Pattern.MULTILINE);
	private static final Pattern EVENT_PATTERN = Pattern.compile("^[\\s]*(○|O|◉|●|⦿)[\\s]*(.+)$", Pattern.MULTILINE);
	private static final Pattern NOTE_PATTERN = Pattern.compile("^[\\s]*[-–—][\\s]*(.+)$", Pattern.MULTILINE);
	private static final Pattern EMOTION_PATTERN = Pattern.compile("(?:feeling|felt|emotion|mood|happy|sad|anxious|excited|worried|calm|stressed|grateful|angry|frustrated|joyful|peaceful|overwhelmed)[\\s]*:?[\\s]*(.+?)(?:\\.|$)", Pattern.CASE_INSENSITIVE);

	/**
	 * Check if line is a task (contains •, X, /, or -)
	 */
	private boolean isTask(String line) {
		return TASK_PATTERN.matcher(line).find();
	}

	/**
	 * Check if line is an event (contains O, ○, ◉, ●, or ⦿)
	 */
	private boolean isEvent(String line) {
		return EVENT_PATTERN.matcher(line).find();
	}

	/**
	 * Check if line is a note (contains -)
	 */
	private boolean isNote(String line) {
		return NOTE_PATTERN.matcher(line).find();
	}

	/**
	 * Check if line is an emotion (contains emotion keywords)
	 */
	private boolean isEmotion(String line) {
		return EMOTION_PATTERN.matcher(line).find();
	}

	/**
	 * Parse task from line using improved pattern matching
	 */
	private Task parseTask(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		Matcher taskMatcher = TASK_PATTERN.matcher(line);
		if (!taskMatcher.find()) {
			return null;
		}

		String symbol = taskMatcher.group(1);
		String content = taskMatcher.group(2).trim();
		
		if (content.isEmpty()) {
			return null;
		}

		Task.TaskStatus status;
		// Determine status based on symbol
		if (symbol.equals("X") || symbol.equals("x")) {
			status = Task.TaskStatus.COMPLETED;
		} else if (symbol.equals("/")) {
			status = Task.TaskStatus.IN_PROGRESS;
		} else {
			// •, ·, or - (dash) = TODO
			status = Task.TaskStatus.TODO;
		}

		return Task.builder()
				.user(user)
				.journalPage(journalPage)
				.content(content)
				.status(status)
				.symbol(symbol)
				.lineNumber(lineNumber)
				.positionHash(positionHash)
				.build();
	}

	/**
	 * Parse event from line using improved pattern matching
	 */
	private Event parseEvent(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		Matcher eventMatcher = EVENT_PATTERN.matcher(line);
		if (!eventMatcher.find()) {
			return null;
		}

		String symbol = eventMatcher.group(1);
		String content = eventMatcher.group(2).trim();
		
		if (content.isEmpty()) {
			return null;
		}

		Event.EventStatus status;
		// Determine status based on symbol
		if (symbol.equals("◉") || symbol.equals("●") || symbol.equals("⦿")) {
			status = Event.EventStatus.COMPLETED;
		} else {
			// O, ○ = SCHEDULED
			status = Event.EventStatus.SCHEDULED;
		}

		// Try to extract date from content
		java.time.LocalDate eventDate = extractDateFromContent(content);

		return Event.builder()
				.user(user)
				.journalPage(journalPage)
				.content(content)
				.eventDate(eventDate)
				.status(status)
				.symbol(symbol)
				.lineNumber(lineNumber)
				.positionHash(positionHash)
				.build();
	}

	/**
	 * Parse note from line (with improved pattern matching for explicit notes)
	 */
	private Note parseNote(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		if (line.isEmpty()) {
			return null;
		}

		String content = line;
		// If line matches note pattern (starts with -), extract content
		Matcher noteMatcher = NOTE_PATTERN.matcher(line);
		if (noteMatcher.find()) {
			content = noteMatcher.group(1).trim();
		}

		if (content.isEmpty()) {
			return null;
		}

		return Note.builder()
				.user(user)
				.journalPage(journalPage)
				.content(content)
				.lineNumber(lineNumber)
				.positionHash(positionHash)
				.build();
	}

	/**
	 * Parse emotion from line using improved pattern matching
	 */
	private Emotion parseEmotion(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		if (line.isEmpty()) {
			return null;
		}

		String content = line;
		// Try to extract emotion content from pattern
		Matcher emotionMatcher = EMOTION_PATTERN.matcher(line);
		if (emotionMatcher.find()) {
			content = emotionMatcher.group(1).trim();
		}

		// Extract emotion type (improved keyword matching)
		String emotionType = extractEmotionType(line);

		return Emotion.builder()
				.user(user)
				.journalPage(journalPage)
				.content(content)
				.emotionType(emotionType)
				.lineNumber(lineNumber)
				.positionHash(positionHash)
				.build();
	}

	/**
	 * Save tasks with duplicate detection
	 */
	private int saveTasks(List<Task> tasks, JournalPage journalPage) {
		int saved = 0;
		for (Task task : tasks) {
			// Check if task already exists (by position hash)
			taskRepository.findByPositionHashAndJournalPageId(
					task.getPositionHash(), journalPage.getId())
					.ifPresentOrElse(
							existing -> {
								// Update existing task
								existing.setContent(task.getContent());
								existing.setStatus(task.getStatus());
								existing.setSymbol(task.getSymbol());
								taskRepository.save(existing);
								log.debug("Updated existing task: {}", existing.getId());
							},
							() -> {
								// Save new task
								taskRepository.save(task);
								log.debug("Saved new task: {}", task.getContent());
							}
					);
			saved++;
		}
		return saved;
	}

	/**
	 * Save events with duplicate detection
	 */
	private int saveEvents(List<Event> events, JournalPage journalPage) {
		int saved = 0;
		for (Event event : events) {
			eventRepository.findByPositionHashAndJournalPageId(
					event.getPositionHash(), journalPage.getId())
					.ifPresentOrElse(
							existing -> {
								existing.setContent(event.getContent());
								existing.setStatus(event.getStatus());
								existing.setEventDate(event.getEventDate());
								existing.setSymbol(event.getSymbol());
								eventRepository.save(existing);
								log.debug("Updated existing event: {}", existing.getId());
							},
							() -> {
								eventRepository.save(event);
								log.debug("Saved new event: {}", event.getContent());
							}
					);
			saved++;
		}
		return saved;
	}

	/**
	 * Save notes with duplicate detection
	 */
	private int saveNotes(List<Note> notes, JournalPage journalPage) {
		int saved = 0;
		for (Note note : notes) {
			noteRepository.findByPositionHashAndJournalPageId(
					note.getPositionHash(), journalPage.getId())
					.ifPresentOrElse(
							existing -> {
								existing.setContent(note.getContent());
								noteRepository.save(existing);
								log.debug("Updated existing note: {}", existing.getId());
							},
							() -> {
								noteRepository.save(note);
								log.debug("Saved new note: {}", note.getContent());
							}
					);
			saved++;
		}
		return saved;
	}

	/**
	 * Save emotions with duplicate detection
	 */
	private int saveEmotions(List<Emotion> emotions, JournalPage journalPage) {
		int saved = 0;
		for (Emotion emotion : emotions) {
			emotionRepository.findByPositionHashAndJournalPageId(
					emotion.getPositionHash(), journalPage.getId())
					.ifPresentOrElse(
							existing -> {
								existing.setContent(emotion.getContent());
								existing.setEmotionType(emotion.getEmotionType());
								emotionRepository.save(existing);
								log.debug("Updated existing emotion: {}", existing.getId());
							},
							() -> {
								emotionRepository.save(emotion);
								log.debug("Saved new emotion: {}", emotion.getContent());
							}
					);
			saved++;
		}
		return saved;
	}

	/**
	 * Generate position hash for duplicate detection
	 */
	private String generatePositionHash(String content, int lineNumber) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			String input = content.toLowerCase().trim() + "|" + lineNumber;
			byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			// Fallback to simple hash
			return String.valueOf((content.toLowerCase().trim() + lineNumber).hashCode());
		}
	}

	/**
	 * Extract date from content (basic implementation)
	 */
	private java.time.LocalDate extractDateFromContent(String content) {
		// Simple date pattern matching (can be enhanced)
		Pattern datePattern = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b");
		Matcher matcher = datePattern.matcher(content);
		
		if (matcher.find()) {
			try {
				int day = Integer.parseInt(matcher.group(1));
				int month = Integer.parseInt(matcher.group(2));
				int year = Integer.parseInt(matcher.group(3));
				if (year < 100) {
					year += 2000; // Convert 2-digit year to 4-digit
				}
				return java.time.LocalDate.of(year, month, day);
			} catch (Exception e) {
				log.debug("Failed to parse date from content: {}", content);
			}
		}
		return null;
	}

	/**
	 * Extract emotion type from content
	 */
	private String extractEmotionType(String content) {
		String lowerContent = content.toLowerCase();
		
		if (lowerContent.contains("happy") || lowerContent.contains("joyful") || lowerContent.contains("😊") || lowerContent.contains("😃")) {
			return "happy";
		} else if (lowerContent.contains("sad") || lowerContent.contains("😢")) {
			return "sad";
		} else if (lowerContent.contains("anxious") || lowerContent.contains("worried") || lowerContent.contains("😰") || lowerContent.contains("😟")) {
			return "anxious";
		} else if (lowerContent.contains("grateful") || lowerContent.contains("🙏")) {
			return "grateful";
		} else if (lowerContent.contains("calm") || lowerContent.contains("peaceful") || lowerContent.contains("😌")) {
			return "calm";
		} else if (lowerContent.contains("stressed") || lowerContent.contains("frustrated") || lowerContent.contains("angry") || lowerContent.contains("😤") || lowerContent.contains("😡")) {
			return "stressed";
		}
		
		return "other";
	}

	/**
	 * Result class for extraction statistics
	 */
	public static class ExtractionResult {
		private final int tasksCount;
		private final int eventsCount;
		private final int notesCount;
		private final int emotionsCount;

		public ExtractionResult(int tasksCount, int eventsCount, int notesCount, int emotionsCount) {
			this.tasksCount = tasksCount;
			this.eventsCount = eventsCount;
			this.notesCount = notesCount;
			this.emotionsCount = emotionsCount;
		}

		public int getTasksCount() { return tasksCount; }
		public int getEventsCount() { return eventsCount; }
		public int getNotesCount() { return notesCount; }
		public int getEmotionsCount() { return emotionsCount; }
	}
}

