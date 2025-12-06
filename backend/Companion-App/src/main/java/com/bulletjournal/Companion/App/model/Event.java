package com.bulletjournal.Companion.App.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

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
	private String content; // Event description

	@Column(name = "event_date")
	private LocalDate eventDate; // Scheduled date for the event

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private EventStatus status = EventStatus.SCHEDULED; // SCHEDULED, COMPLETED

	@Column(name = "symbol")
	private String symbol; // Original symbol: O, ⦿ (filled O), etc.

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

	public enum EventStatus {
		SCHEDULED,  // O (open circle) - scheduled event
		COMPLETED   // ⦿ (filled circle) - completed event
	}
}

