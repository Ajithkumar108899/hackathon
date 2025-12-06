package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/journal/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "APIs for exporting journal data in various formats")
@SecurityRequirement(name = "Bearer Authentication")
public class ExportController {

	private final ExportService exportService;

	@GetMapping("/taskpaper")
	@Operation(summary = "Export tasks as TaskPaper", 
			   description = "Export all tasks in TaskPaper format (.taskpaper file)")
	public ResponseEntity<String> exportTaskPaper(@AuthenticationPrincipal User user) {
		try {
			String taskPaper = exportService.exportToTaskPaper(user.getId());
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentDispositionFormData("attachment", 
					"bullet-journal-tasks-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".taskpaper");
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(taskPaper);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error exporting TaskPaper: " + e.getMessage());
		}
	}

	@GetMapping("/markdown")
	@Operation(summary = "Export notes and emotions as Markdown", 
			   description = "Export all notes and emotions in Markdown format (.md file)")
	public ResponseEntity<String> exportMarkdown(@AuthenticationPrincipal User user) {
		try {
			String markdown = exportService.exportToMarkdown(user.getId());
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentDispositionFormData("attachment", 
					"bullet-journal-notes-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".md");
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(markdown);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error exporting Markdown: " + e.getMessage());
		}
	}

	@GetMapping("/all")
	@Operation(summary = "Export all content as Markdown", 
			   description = "Export all tasks, events, notes, and emotions in Markdown format")
	public ResponseEntity<String> exportAll(@AuthenticationPrincipal User user) {
		try {
			String markdown = exportService.exportAllToMarkdown(user.getId());
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_PLAIN);
			headers.setContentDispositionFormData("attachment", 
					"bullet-journal-complete-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".md");
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(markdown);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error exporting: " + e.getMessage());
		}
	}
}

