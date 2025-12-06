package com.bulletjournal.Companion.App.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_pages")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalPage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String imagePath; // Path to stored image file

	@Column(nullable = false)
	private String originalFilename; // Original filename from upload

	@Column(columnDefinition = "TEXT")
	private String extractedText; // Raw OCR extracted text

	@Column(nullable = false)
	@Builder.Default
	private Integer pageNumber = 1; // Page number in journal

	@Column(name = "thread_id")
	private String threadId; // For linking related pages (optional threading feature)

	@Column(nullable = false, updatable = false)
	private LocalDateTime scannedAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		scannedAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}

