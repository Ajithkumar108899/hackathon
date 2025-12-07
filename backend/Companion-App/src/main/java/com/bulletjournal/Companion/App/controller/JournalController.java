package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.dto.ExtractedDataResponse;
import com.bulletjournal.Companion.App.dto.*;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.service.JournalEntryService;
import com.bulletjournal.Companion.App.service.JournalPageService;
import com.bulletjournal.Companion.App.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
	private final JournalEntryService journalEntryService;

	@PostMapping(value = "/scan", consumes = "multipart/form-data")
	@Operation(
		summary = "Scan journal page(s)", 
		description = "Upload and scan one or more handwritten journal page images. " +
				"Each image will be processed using OCR to extract text, and then automatically " +
				"detect tasks (•, X, /), events (O, ⦿), notes, and emotions. " +
				"**To upload multiple images:** In Swagger UI, click 'Choose File' and select multiple files " +
				"by holding Ctrl (Windows) or Cmd (Mac) while clicking. " +
				"**Requires authentication token in header.**"
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
			@AuthenticationPrincipal User user,
			@Valid @ModelAttribute ScanRequest request) {
		try {
			// Get userId from authenticated user (from token)
			List<ScanResponse> responses = journalPageService.scanAndSavePage(user.getId(), request);
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
		description = "Retrieve all scanned journal pages for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<List<ScanResponse>> getAllPages(@AuthenticationPrincipal User user) {
		List<ScanResponse> pages = journalPageService.getUserPages(user.getId());
		return ResponseEntity.ok(pages);
	}

	@GetMapping("/pages/{pageId}")
	@Operation(
		summary = "Get journal page by ID", 
		description = "Retrieve a specific journal page by its ID for the authenticated user. **Requires authentication token in header.**"
	)
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
	@Operation(
		summary = "Search journal entries", 
		description = "Search across tasks, events, notes, and emotions for the authenticated user. **Requires authentication token in header.**"
	)
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

	// ========== Journal Entries CRUD APIs ==========

	@PostMapping("/entries")
	@Operation(
		summary = "Create a new journal entry",
		description = "Create a new journal entry (task, note, event, or habit) for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<JournalEntryResponse> createEntry(
			@AuthenticationPrincipal User user,
			@Valid @RequestBody JournalEntryRequest request) {
		try {
			JournalEntryResponse response = journalEntryService.createEntry(user.getId(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/getAllEntries")
	@Operation(
		summary = "Get all journal entries",
		description = "Retrieve all journal entries (tasks, notes, events, habits) for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<List<JournalEntryResponse>> getAllEntries(@AuthenticationPrincipal User user) {
		try {
			List<JournalEntryResponse> entries = journalEntryService.getAllEntries(user.getId());
			return ResponseEntity.ok(entries);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/entries/{id}")
	@Operation(
		summary = "Get journal entry by ID",
		description = "Retrieve a specific journal entry by its ID for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<JournalEntryResponse> getEntryById(
			@AuthenticationPrincipal User user,
			@PathVariable String id) {
		try {
			JournalEntryResponse entry = journalEntryService.getEntryById(user.getId(), id);
			return ResponseEntity.ok(entry);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

        @PutMapping("/entries/{id}")
        @Operation(
            summary = "Update journal entry",
            description = "Update an existing journal entry for the authenticated user. If the entry type is changed, the entry will be deleted from the old table and created in the new table. **Requires authentication token in header.**"
        )
        public ResponseEntity<JournalEntryResponse> updateEntry(
                @AuthenticationPrincipal User user,
                @PathVariable String id,
                @Valid @RequestBody JournalEntryRequest request) {
            try {
                JournalEntryResponse response = journalEntryService.updateEntry(user.getId(), id, request);
                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        
        @PutMapping("/updateEntries/{id}")
        @Operation(
            summary = "Update journal entry (alternative endpoint)",
            description = "Update an existing journal entry for the authenticated user. If the entry type is changed, the entry will be deleted from the old table and created in the new table. **Requires authentication token in header.**"
        )
        public ResponseEntity<JournalEntryResponse> updateEntryAlternative(
                @AuthenticationPrincipal User user,
                @PathVariable String id,
                @Valid @RequestBody JournalEntryRequest request) {
            try {
                JournalEntryResponse response = journalEntryService.updateEntry(user.getId(), id, request);
                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

	@DeleteMapping("/entries/{id}")
	@Operation(
		summary = "Delete journal entry",
		description = "Delete a journal entry by its ID for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<Void> deleteEntry(
			@AuthenticationPrincipal User user,
			@PathVariable String id) {
		try {
			journalEntryService.deleteEntry(user.getId(), id);
			return ResponseEntity.ok().build();
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/deleteEntriesById/{id}")
	@Operation(
		summary = "Delete journal entry by ID (alternative endpoint)",
		description = "Delete a journal entry by its ID for the authenticated user. The system will automatically check all tables (tasks, notes, events, emotions) to find and delete the entry. **Requires authentication token in header.**"
	)
	public ResponseEntity<Void> deleteEntryById(
			@AuthenticationPrincipal User user,
			@PathVariable String id) {
		try {
			journalEntryService.deleteEntry(user.getId(), id);
			return ResponseEntity.ok().build();
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PatchMapping("/entries/{id}/toggle")
	@Operation(
		summary = "Toggle entry completion status",
		description = "Toggle the completion status of a journal entry (task, note, or event) for the authenticated user. **Requires authentication token in header.**"
	)
	public ResponseEntity<JournalEntryResponse> toggleComplete(
			@AuthenticationPrincipal User user,
			@PathVariable String id,
			@RequestBody(required = false) ToggleRequest request) {
		try {
			Boolean completed = request != null ? request.getCompleted() : null;
			JournalEntryResponse response = journalEntryService.toggleComplete(user.getId(), id, completed);
			return ResponseEntity.ok(response);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/extractedData")
	@Operation(
		summary = "Get extracted data for Extracted Data View",
		description = "Retrieve extracted journal entries (tasks, notes, events, emotions) for the authenticated user. " +
				"If journalPageId is provided, returns only entries from that specific scan. " +
				"If not provided, returns all entries from scanned images (excludes manual entries). " +
				"Returns title, type, symbol, status, and created date. **Requires authentication token in header.**"
	)
	public ResponseEntity<List<ExtractedDataResponse>> getExtractedData(
			@AuthenticationPrincipal User user,
			@RequestParam(required = false) Long journalPageId) {
		try {
			List<ExtractedDataResponse> extractedData = journalEntryService.getExtractedData(user.getId(), journalPageId);
			return ResponseEntity.ok(extractedData);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}

