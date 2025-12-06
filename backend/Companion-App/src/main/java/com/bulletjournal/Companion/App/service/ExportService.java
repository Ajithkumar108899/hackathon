package com.bulletjournal.Companion.App.service;

import com.bulletjournal.Companion.App.model.*;
import com.bulletjournal.Companion.App.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

	private final TaskRepository taskRepository;
	private final EventRepository eventRepository;
	private final NoteRepository noteRepository;
	private final EmotionRepository emotionRepository;

	/**
	 * Export tasks in TaskPaper format
	 */
	public String exportToTaskPaper(Long userId) {
		List<Task> tasks = taskRepository.findByUserId(userId);
		
		if (tasks.isEmpty()) {
			return "# Tasks\n\nNo tasks found.\n";
		}

		StringBuilder taskPaper = new StringBuilder();
		taskPaper.append("# Bullet Journal Tasks\n\n");

		// Group tasks by status
		List<Task> todoTasks = tasks.stream()
				.filter(t -> t.getStatus() == Task.TaskStatus.TODO)
				.collect(Collectors.toList());
		
		List<Task> inProgressTasks = tasks.stream()
				.filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
				.collect(Collectors.toList());
		
		List<Task> completedTasks = tasks.stream()
				.filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED)
				.collect(Collectors.toList());

		// Write TODO tasks
		if (!todoTasks.isEmpty()) {
			taskPaper.append("## TODO\n\n");
			for (Task task : todoTasks) {
				taskPaper.append("- ").append(task.getContent());
				if (task.getJournalPage() != null) {
					taskPaper.append(" @page(").append(task.getJournalPage().getPageNumber()).append(")");
				}
				taskPaper.append("\n");
			}
			taskPaper.append("\n");
		}

		// Write IN_PROGRESS tasks
		if (!inProgressTasks.isEmpty()) {
			taskPaper.append("## IN PROGRESS\n\n");
			for (Task task : inProgressTasks) {
				taskPaper.append("- ").append(task.getContent()).append(" @inprogress");
				if (task.getJournalPage() != null) {
					taskPaper.append(" @page(").append(task.getJournalPage().getPageNumber()).append(")");
				}
				taskPaper.append("\n");
			}
			taskPaper.append("\n");
		}

		// Write COMPLETED tasks
		if (!completedTasks.isEmpty()) {
			taskPaper.append("## COMPLETED\n\n");
			for (Task task : completedTasks) {
				taskPaper.append("- ").append(task.getContent()).append(" @done");
				if (task.getJournalPage() != null) {
					taskPaper.append(" @page(").append(task.getJournalPage().getPageNumber()).append(")");
				}
				if (task.getUpdatedAt() != null) {
					taskPaper.append(" @completed(")
							.append(task.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
							.append(")");
				}
				taskPaper.append("\n");
			}
			taskPaper.append("\n");
		}

		return taskPaper.toString();
	}

	/**
	 * Export notes and emotions in Markdown format
	 */
	public String exportToMarkdown(Long userId) {
		List<Note> notes = noteRepository.findByUserId(userId);
		List<Emotion> emotions = emotionRepository.findByUserId(userId);

		StringBuilder markdown = new StringBuilder();
		markdown.append("# Bullet Journal - Notes & Emotions\n\n");

		// Export Notes
		if (!notes.isEmpty()) {
			markdown.append("## Notes\n\n");
			
			// Group by date
			notes.stream()
					.collect(Collectors.groupingBy(note -> 
							note.getCreatedAt() != null ? 
									note.getCreatedAt().toLocalDate() : LocalDate.now()))
					.entrySet().stream()
					.sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())) // Latest first
					.forEach(entry -> {
						markdown.append("### ").append(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
						for (Note note : entry.getValue()) {
							markdown.append("- ").append(note.getContent());
							if (note.getJournalPage() != null) {
								markdown.append(" *(Page ").append(note.getJournalPage().getPageNumber()).append(")*");
							}
							markdown.append("\n");
						}
						markdown.append("\n");
					});
		} else {
			markdown.append("## Notes\n\nNo notes found.\n\n");
		}

		// Export Emotions
		if (!emotions.isEmpty()) {
			markdown.append("## Emotions\n\n");
			
			// Group by date
			emotions.stream()
					.collect(Collectors.groupingBy(emotion -> 
							emotion.getCreatedAt() != null ? 
									emotion.getCreatedAt().toLocalDate() : LocalDate.now()))
					.entrySet().stream()
					.sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())) // Latest first
					.forEach(entry -> {
						markdown.append("### ").append(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
						for (Emotion emotion : entry.getValue()) {
							markdown.append("- **");
							if (emotion.getEmotionType() != null && !emotion.getEmotionType().isEmpty()) {
								markdown.append(emotion.getEmotionType().toUpperCase()).append("**: ");
							}
							markdown.append(emotion.getContent());
							if (emotion.getJournalPage() != null) {
								markdown.append(" *(Page ").append(emotion.getJournalPage().getPageNumber()).append(")*");
							}
							markdown.append("\n");
						}
						markdown.append("\n");
					});
		} else {
			markdown.append("## Emotions\n\nNo emotions recorded.\n\n");
		}

		return markdown.toString();
	}

	/**
	 * Export all content (tasks, notes, emotions, events) in Markdown format
	 */
	public String exportAllToMarkdown(Long userId) {
		List<Task> tasks = taskRepository.findByUserId(userId);
		List<Event> events = eventRepository.findByUserId(userId);
		List<Note> notes = noteRepository.findByUserId(userId);
		List<Emotion> emotions = emotionRepository.findByUserId(userId);

		StringBuilder markdown = new StringBuilder();
		markdown.append("# Bullet Journal - Complete Export\n\n");
		markdown.append("*Generated on: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("*\n\n");

		// Export Tasks
		markdown.append("## Tasks\n\n");
		if (!tasks.isEmpty()) {
			for (Task task : tasks) {
				String status = switch (task.getStatus()) {
					case TODO -> "•";
					case IN_PROGRESS -> "/";
					case COMPLETED -> "X";
				};
				markdown.append("- ").append(status).append(" ").append(task.getContent());
				if (task.getJournalPage() != null) {
					markdown.append(" *(Page ").append(task.getJournalPage().getPageNumber()).append(")*");
				}
				markdown.append("\n");
			}
		} else {
			markdown.append("No tasks found.\n");
		}
		markdown.append("\n");

		// Export Events
		markdown.append("## Events\n\n");
		if (!events.isEmpty()) {
			for (Event event : events) {
				String status = event.getStatus() == Event.EventStatus.COMPLETED ? "⦿" : "O";
				markdown.append("- ").append(status).append(" ").append(event.getContent());
				if (event.getEventDate() != null) {
					markdown.append(" *(").append(event.getEventDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(")*");
				}
				if (event.getJournalPage() != null) {
					markdown.append(" *(Page ").append(event.getJournalPage().getPageNumber()).append(")*");
				}
				markdown.append("\n");
			}
		} else {
			markdown.append("No events found.\n");
		}
		markdown.append("\n");

		// Export Notes
		markdown.append("## Notes\n\n");
		if (!notes.isEmpty()) {
			for (Note note : notes) {
				markdown.append("- ").append(note.getContent());
				if (note.getJournalPage() != null) {
					markdown.append(" *(Page ").append(note.getJournalPage().getPageNumber()).append(")*");
				}
				markdown.append("\n");
			}
		} else {
			markdown.append("No notes found.\n");
		}
		markdown.append("\n");

		// Export Emotions
		markdown.append("## Emotions\n\n");
		if (!emotions.isEmpty()) {
			for (Emotion emotion : emotions) {
				markdown.append("- ");
				if (emotion.getEmotionType() != null && !emotion.getEmotionType().isEmpty()) {
					markdown.append("**").append(emotion.getEmotionType().toUpperCase()).append("**: ");
				}
				markdown.append(emotion.getContent());
				if (emotion.getJournalPage() != null) {
					markdown.append(" *(Page ").append(emotion.getJournalPage().getPageNumber()).append(")*");
				}
				markdown.append("\n");
			}
		} else {
			markdown.append("No emotions recorded.\n");
		}

		return markdown.toString();
	}
}

