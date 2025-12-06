package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResponse {
	
	private Long journalPageId;
	private String imagePath;
	private String originalFilename;
	private Integer pageNumber;
	private String threadId;
	private LocalDateTime scannedAt;
	private String message;
	private String extractedText; // Optional: include extracted text in response
}

