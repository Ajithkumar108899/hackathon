package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.dto.*;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.service.JournalPageService;
import com.bulletjournal.Companion.App.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
@Tag(name = "Journal Management", description = "APIs for scanning and managing journal pages")
@SecurityRequirement(name = "Bearer Authentication")
public class JournalController {

	private final JournalPageService journalPageService;
	private final SearchService searchService;

	@PostMapping("/scan")
	@Operation(summary = "Scan journal page", 
			   description = "Upload and scan a handwritten journal page image")
	public ResponseEntity<ScanResponse> scanPage(
			@AuthenticationPrincipal User user,
			@Valid @ModelAttribute ScanRequest request) {
		try {
			ScanResponse response = journalPageService.scanAndSavePage(user.getId(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ScanResponse.builder()
							.message("Error saving file: " + e.getMessage())
							.build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ScanResponse.builder()
							.message(e.getMessage())
							.build());
		}
	}

	@GetMapping("/pages")
	@Operation(summary = "Get all journal pages", 
			   description = "Retrieve all scanned journal pages for the current user")
	public ResponseEntity<List<ScanResponse>> getAllPages(@AuthenticationPrincipal User user) {
		List<ScanResponse> pages = journalPageService.getUserPages(user.getId());
		return ResponseEntity.ok(pages);
	}

	@GetMapping("/pages/{pageId}")
	@Operation(summary = "Get journal page by ID", 
			   description = "Retrieve a specific journal page by its ID")
	public ResponseEntity<ScanResponse> getPageById(
			@AuthenticationPrincipal User user,
			@PathVariable Long pageId) {
		try {
			ScanResponse page = journalPageService.getPageById(pageId, user.getId());
			return ResponseEntity.ok(page);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ScanResponse.builder()
							.message(e.getMessage())
							.build());
		}
	}

	@GetMapping("/search")
	@Operation(summary = "Search journal entries", 
			   description = "Search across tasks, events, notes, and emotions")
	public ResponseEntity<SearchResponse> search(
			@AuthenticationPrincipal User user,
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String status) {
		SearchRequest request = new SearchRequest();
		request.setQuery(query);
		request.setType(type);
		request.setStatus(status);
		
		SearchResponse response = searchService.search(user.getId(), request);
		return ResponseEntity.ok(response);
	}
}

