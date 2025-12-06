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

			// Detect and parse based on symbols
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
			} else {
				// Default to note if no symbol detected
				Note note = parseNote(line, lineNumber, positionHash, journalPage, user);
				if (note != null) {
					notes.add(note);
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

	/**
	 * Check if line is a task (contains •, X, or /)
	 */
	private boolean isTask(String line) {
		// Pattern: • (dot), X (completed), / (in progress)
		Pattern taskPattern = Pattern.compile("^[•·Xx/]\\s+.*", Pattern.CASE_INSENSITIVE);
		return taskPattern.matcher(line).find();
	}

	/**
	 * Check if line is an event (contains O or ⦿)
	 */
	private boolean isEvent(String line) {
		// Pattern: O (open circle), ⦿ (filled circle), or "O " at start
		Pattern eventPattern = Pattern.compile("^[O⦿○◯]\\s+.*", Pattern.CASE_INSENSITIVE);
		return eventPattern.matcher(line).find();
	}

	/**
	 * Check if line is an emotion (contains emotion keywords or emoji)
	 */
	private boolean isEmotion(String line) {
		// Common emotion keywords
		String[] emotionKeywords = {
			"happy", "sad", "anxious", "grateful", "excited", "worried",
			"calm", "stressed", "joyful", "frustrated", "peaceful", "angry",
			"😊", "😢", "😰", "🙏", "😃", "😟", "😌", "😤", "😡"
		};
		
		String lowerLine = line.toLowerCase();
		for (String keyword : emotionKeywords) {
			if (lowerLine.contains(keyword.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parse task from line
	 */
	private Task parseTask(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		// Extract symbol and content
		String symbol = "";
		String content = line;
		Task.TaskStatus status = Task.TaskStatus.TODO;

		// Detect symbol and status
		if (line.matches("^[Xx]\\s+.*")) {
			symbol = "X";
			status = Task.TaskStatus.COMPLETED;
			content = line.substring(1).trim();
		} else if (line.matches("^[/]\\s+.*")) {
			symbol = "/";
			status = Task.TaskStatus.IN_PROGRESS;
			content = line.substring(1).trim();
		} else if (line.matches("^[•·]\\s+.*")) {
			symbol = "•";
			status = Task.TaskStatus.TODO;
			content = line.substring(1).trim();
		}

		if (content.isEmpty()) {
			return null;
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
	 * Parse event from line
	 */
	private Event parseEvent(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		String symbol = "";
		String content = line;
		Event.EventStatus status = Event.EventStatus.SCHEDULED;

		// Detect symbol and status
		if (line.matches("^[⦿●]\\s+.*")) {
			symbol = "⦿";
			status = Event.EventStatus.COMPLETED;
			content = line.substring(1).trim();
		} else if (line.matches("^[O○◯]\\s+.*")) {
			symbol = "O";
			status = Event.EventStatus.SCHEDULED;
			content = line.substring(1).trim();
		}

		if (content.isEmpty()) {
			return null;
		}

		// Try to extract date from content (simple pattern matching)
		// This is a basic implementation - can be enhanced
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
	 * Parse note from line
	 */
	private Note parseNote(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		if (line.isEmpty()) {
			return null;
		}

		return Note.builder()
				.user(user)
				.journalPage(journalPage)
				.content(line)
				.lineNumber(lineNumber)
				.positionHash(positionHash)
				.build();
	}

	/**
	 * Parse emotion from line
	 */
	private Emotion parseEmotion(String line, int lineNumber, String positionHash, JournalPage journalPage, User user) {
		if (line.isEmpty()) {
			return null;
		}

		// Extract emotion type (simple keyword matching)
		String emotionType = extractEmotionType(line);

		return Emotion.builder()
				.user(user)
				.journalPage(journalPage)
				.content(line)
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

