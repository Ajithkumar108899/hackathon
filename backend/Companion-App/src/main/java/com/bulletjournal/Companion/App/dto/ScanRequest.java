package com.bulletjournal.Companion.App.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Schema(description = "Request for scanning a journal page")
public class ScanRequest {
	
	@NotNull(message = "Image file is required")
	@Schema(
		description = "Journal page image file(s) (JPG, PNG, etc.). " +
				"You can select multiple files by holding Ctrl (Windows) or Cmd (Mac) while clicking 'Choose File'.",
		type = "string",
		format = "binary",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	private List<MultipartFile> image;
	
	@Min(value = 1, message = "Page number must be at least 1")
	@Schema(description = "Page number (optional, defaults to 1). If multiple images, page numbers will be auto-incremented.", example = "1")
	private Integer pageNumber;
	
	@Schema(description = "Thread ID for linking related pages (optional)", example = "2025-12-06")
	private String threadId; // Optional: for linking related pages
}

