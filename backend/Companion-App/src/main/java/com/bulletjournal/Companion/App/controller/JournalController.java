package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.dto.*;
import com.bulletjournal.Companion.App.service.JournalPageService;
import com.bulletjournal.Companion.App.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
@Tag(name = "Journal Management", description = "APIs for scanning and managing journal pages")
//@SecurityRequirement(name = "Bearer Authentication")
public class JournalController {

	private final JournalPageService journalPageService;
	private final SearchService searchService;

	@PostMapping(value = "/scan", consumes = "multipart/form-data")
	@Operation(
		summary = "Scan journal page(s)", 
		description = "Upload and scan one or more handwritten journal page images. " +
				"Each image will be processed using OCR to extract text, and then automatically " +
				"detect tasks (•, X, /), events (O, ⦿), notes, and emotions. " +
				"**To upload multiple images:** In Swagger UI, click 'Choose File' and select multiple files " +
				"by holding Ctrl (Windows) or Cmd (Mac) while clicking. " +
				"Note: This endpoint does not require authentication.",
		security = {} // Explicitly disable security for this endpoint
	)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Journal page image(s) and metadata. You can select multiple image files at once.",
		required = true,
		content = @Content(
			mediaType = "multipart/form-data",
			schema = @Schema(implementation = ScanRequest.class)
		)
	)
	public ResponseEntity<List<ScanResponse>> scanPage(
			@Valid @ModelAttribute ScanRequest request) {
		try {
			// Use default user ID (1) or create a guest user for unauthenticated scans
			// You may want to modify this based on your requirements
			Long userId = 1L; // Default user ID for unauthenticated scans
			List<ScanResponse> responses = journalPageService.scanAndSavePage(userId, request);
			return ResponseEntity.status(HttpStatus.CREATED).body(responses);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(List.of(ScanResponse.builder()
							.message("Error saving file: " + e.getMessage())
							.build()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(List.of(ScanResponse.builder()
							.message(e.getMessage())
							.build()));
		}
	}

	@GetMapping("/pages")
	@Operation(
		summary = "Get all journal pages", 
		description = "Retrieve all scanned journal pages. Note: This endpoint does not require authentication.",
		security = {} // Explicitly disable security for this endpoint
	)
	public ResponseEntity<List<ScanResponse>> getAllPages() {
		Long userId = 7L; // Default user ID for unauthenticated access
		List<ScanResponse> pages = journalPageService.getUserPages(userId);
		return ResponseEntity.ok(pages);
	}

	@GetMapping("/pages/{pageId}")
	@Operation(
		summary = "Get journal page by ID", 
		description = "Retrieve a specific journal page by its ID. Note: This endpoint does not require authentication.",
		security = {} // Explicitly disable security for this endpoint
	)
	public ResponseEntity<ScanResponse> getPageById(@PathVariable Long pageId) {
		try {
			Long userId = 1L; // Default user ID for unauthenticated access
			ScanResponse page = journalPageService.getPageById(pageId, userId);
			return ResponseEntity.ok(page);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ScanResponse.builder()
							.message(e.getMessage())
							.build());
		}
	}

	@GetMapping("/search")
	@Operation(
		summary = "Search journal entries", 
		description = "Search across tasks, events, notes, and emotions. Note: This endpoint does not require authentication.",
		security = {} // Explicitly disable security for this endpoint
	)
	public ResponseEntity<SearchResponse> search(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String status) {
		Long userId = 1L; // Default user ID for unauthenticated access
		SearchRequest request = new SearchRequest();
		request.setQuery(query);
		request.setType(type);
		request.setStatus(status);
		
		SearchResponse response = searchService.search(userId, request);
		return ResponseEntity.ok(response);
	}
}

