package com.bulletjournal.Companion.App.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "journal_page_id", nullable = false)
	private JournalPage journalPage;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content; // Task description

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private TaskStatus status = TaskStatus.TODO; // TODO, IN_PROGRESS, COMPLETED

	@Column(name = "symbol")
	private String symbol; // Original symbol: •, X, /, etc.

	@Column(name = "line_number")
	private Integer lineNumber; // Line number on the page (for duplicate detection)

	@Column(name = "position_hash")
	private String positionHash; // Hash of position for duplicate detection

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public enum TaskStatus {
		TODO,           // • (dot) - new task
		IN_PROGRESS,    // / (slash) - task in progress
		COMPLETED       // X - task completed
	}
}

