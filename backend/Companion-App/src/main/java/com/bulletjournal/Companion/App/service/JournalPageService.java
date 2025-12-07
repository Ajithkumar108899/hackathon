package com.bulletjournal.Companion.App.service;

import com.bulletjournal.Companion.App.dto.ScanRequest;
import com.bulletjournal.Companion.App.dto.ScanResponse;
import com.bulletjournal.Companion.App.model.JournalPage;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.repository.JournalPageRepository;
import com.bulletjournal.Companion.App.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalPageService {

	private final JournalPageRepository journalPageRepository;
	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;
	private final OcrService ocrService;
	private final ContentExtractionService contentExtractionService;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public List<ScanResponse> scanAndSavePage(Long userId, ScanRequest request) throws IOException {
		// Get user from database (userId comes from authenticated token)
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found. Please ensure you are authenticated."));

		// Validate images list
		if (request.getImage() == null || request.getImage().isEmpty()) {
			throw new IllegalArgumentException("At least one image file is required");
		}

		// Process each image in the list
		List<ScanResponse> responses = new java.util.ArrayList<>();
		int basePageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
		
		for (int i = 0; i < request.getImage().size(); i++) {
			org.springframework.web.multipart.MultipartFile imageFile = request.getImage().get(i);
			
			try {
				// Store image file
				String imagePath = fileStorageService.storeFile(imageFile, user.getId());
				
				// Check if OCR is available before attempting extraction
				boolean ocrAvailable = ocrService.isOcrAvailable();
				if (!ocrAvailable) {
					log.warn("Tesseract OCR is not available. Skipping OCR extraction for image {}.", i + 1);
				}
				
				// Perform OCR extraction
				String extractedText = "";
				try {
					if (ocrAvailable) {
						File file = fileStorageService.getFilePath(imagePath).toFile();
						extractedText = ocrService.extractText(file);
						if (extractedText != null && !extractedText.trim().isEmpty()) {
							log.info("OCR extraction completed for image {}: {} characters", i + 1, extractedText.length());
						} else {
							log.warn("OCR extraction returned empty text for image {}. This may indicate image quality issues or OCR configuration problems.", i + 1);
							extractedText = "";
						}
					} else {
						extractedText = "";
					}
				} catch (TesseractException | IOException | Error e) {
					log.error("OCR extraction failed for image {}: {}", i + 1, e.getMessage(), e);
					extractedText = "";
					log.warn("Continuing without OCR for image {}. Image saved but no text extracted.", i + 1);
				}

				// Calculate page number (increment for each image)
				int pageNumber = basePageNumber + i;

				// Create JournalPage entity
				JournalPage journalPage = JournalPage.builder()
						.user(user)
						.imagePath(imagePath)
						.originalFilename(imageFile.getOriginalFilename())
						.extractedText(extractedText)
						.pageNumber(pageNumber)
						.threadId(request.getThreadId())
						.build();

				// Save to database
				journalPage = journalPageRepository.save(journalPage);
				log.info("Journal page saved: ID={}, User={}, Page={}, TextLength={}", 
						journalPage.getId(), user.getId(), journalPage.getPageNumber(), extractedText.length());

				// Extract and save content (tasks, events, notes, emotions)
				ContentExtractionService.ExtractionResult extractionResult = null;
				if (!extractedText.isEmpty() && !extractedText.startsWith("OCR extraction failed")) {
					try {
						extractionResult = contentExtractionService.extractAndSaveContent(extractedText, journalPage, user);
						log.info("Content extraction completed for page {}: {} tasks, {} events, {} notes, {} emotions",
								journalPage.getPageNumber(),
								extractionResult.getTasksCount(), extractionResult.getEventsCount(),
								extractionResult.getNotesCount(), extractionResult.getEmotionsCount());
					} catch (Exception e) {
						log.error("Content extraction failed for page {}: {}", journalPage.getPageNumber(), e.getMessage(), e);
					}
				}

				// Build response message
				String message = "Page scanned and saved successfully.";
				if (extractedText != null && !extractedText.isEmpty()) {
					message += " OCR extracted " + extractedText.length() + " characters";
					if (extractionResult != null) {
						message += String.format(". Extracted: %d tasks, %d events, %d notes, %d emotions",
								extractionResult.getTasksCount(), extractionResult.getEventsCount(),
								extractionResult.getNotesCount(), extractionResult.getEmotionsCount());
					}
				} else {
					// Reuse the ocrAvailable variable declared earlier
					if (!ocrAvailable) {
						message += " Note: Tesseract OCR is not installed. ";
						message += "Please install Tesseract OCR to enable text extraction. ";
						message += "Windows: Download from https://github.com/UB-Mannheim/tesseract/wiki or use 'choco install tesseract'. ";
						message += "Linux: 'sudo apt-get install tesseract-ocr'. ";
						message += "Mac: 'brew install tesseract'.";
					} else {
						message += " Note: OCR extraction returned empty text. This may indicate image quality issues.";
					}
				}

				// Build response for this image
				ScanResponse response = ScanResponse.builder()
						.journalPageId(journalPage.getId())
						.imagePath(journalPage.getImagePath())
						.originalFilename(journalPage.getOriginalFilename())
						.pageNumber(journalPage.getPageNumber())
						.threadId(journalPage.getThreadId())
						.scannedAt(journalPage.getScannedAt())
						.extractedText(extractedText)
						.message(message)
						.build();
				
				responses.add(response);
			} catch (Exception e) {
				log.error("Error processing image {}: {}", i + 1, e.getMessage(), e);
				// Add error response for this image
				responses.add(ScanResponse.builder()
						.message("Error processing image " + (i + 1) + ": " + e.getMessage())
						.build());
			}
		}

		return responses;
	}

	public List<ScanResponse> getUserPages(Long userId) {
		List<JournalPage> pages = journalPageRepository.findByUserId(userId);
		return pages.stream()
				.map(page -> ScanResponse.builder()
						.journalPageId(page.getId())
						.imagePath(page.getImagePath())
						.originalFilename(page.getOriginalFilename())
						.pageNumber(page.getPageNumber())
						.threadId(page.getThreadId())
						.scannedAt(page.getScannedAt())
						.build())
				.collect(Collectors.toList());
	}

	public ScanResponse getPageById(Long pageId, Long userId) {
		JournalPage page = journalPageRepository.findByIdAndUserId(pageId, userId)
				.orElseThrow(() -> new RuntimeException("Journal page not found"));

		return ScanResponse.builder()
				.journalPageId(page.getId())
				.imagePath(page.getImagePath())
				.originalFilename(page.getOriginalFilename())
				.pageNumber(page.getPageNumber())
				.threadId(page.getThreadId())
				.scannedAt(page.getScannedAt())
				.extractedText(page.getExtractedText())
				.build();
	}

	/**
	 * Get JournalPage entity by ID (for internal use)
	 */
	public JournalPage getJournalPageById(Long pageId, Long userId) {
		return journalPageRepository.findByIdAndUserId(pageId, userId)
				.orElseThrow(() -> new RuntimeException("Journal page not found"));
	}
}

