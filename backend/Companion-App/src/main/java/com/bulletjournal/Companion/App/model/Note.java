package com.bulletjournal.Companion.App.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

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
	private String content; // Note content

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private NoteStatus status = NoteStatus.SCHEDULED; // Note completion status

	@Column(name = "line_number")
	private Integer lineNumber; // Line number on the page

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

	public enum NoteStatus {
		SCHEDULED,  // O (open circle) - scheduled event
		COMPLETED   // ⦿ (filled circle) - completed event
	}
}

