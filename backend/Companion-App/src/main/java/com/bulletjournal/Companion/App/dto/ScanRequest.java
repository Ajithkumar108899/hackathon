package com.bulletjournal.Companion.App.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ScanRequest {
	
	@NotNull(message = "Image file is required")
	private MultipartFile image;
	
	@Min(value = 1, message = "Page number must be at least 1")
	private Integer pageNumber;
	
	private String threadId; // Optional: for linking related pages
}

