package com.bulletjournal.Companion.App.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

	@Value("${journal.image.storage-path}")
	private String storagePath;

	@Value("${journal.image.allowed-extensions}")
	private String allowedExtensions;

	public String storeFile(MultipartFile file, Long userId) throws IOException {
		// Validate file
		validateFile(file);

		// Create storage directory if it doesn't exist
		Path storageDir = Paths.get(storagePath, userId.toString());
		Files.createDirectories(storageDir);

		// Generate unique filename
		String originalFilename = file.getOriginalFilename();
		String extension = getFileExtension(originalFilename);
		String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
		Path filePath = storageDir.resolve(uniqueFilename);

		// Save file
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
		log.info("File saved: {}", filePath);

		// Return relative path for database storage
		return Paths.get(userId.toString(), uniqueFilename).toString().replace("\\", "/");
	}

	public Path getFilePath(String relativePath) {
		return Paths.get(storagePath, relativePath);
	}

	public boolean fileExists(String relativePath) {
		Path filePath = getFilePath(relativePath);
		return Files.exists(filePath);
	}

	public void deleteFile(String relativePath) throws IOException {
		Path filePath = getFilePath(relativePath);
		if (Files.exists(filePath)) {
			Files.delete(filePath);
			log.info("File deleted: {}", filePath);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}

		String filename = file.getOriginalFilename();
		if (filename == null) {
			throw new IllegalArgumentException("Filename is null");
		}

		String extension = getFileExtension(filename).toLowerCase();
		List<String> allowed = Arrays.asList(allowedExtensions.toLowerCase().split(","));

		if (!allowed.contains(extension)) {
			throw new IllegalArgumentException(
					"File extension not allowed. Allowed: " + allowedExtensions);
		}

		// Check file size (already handled by Spring, but double-check)
		long maxSize = 10 * 1024 * 1024; // 10MB
		if (file.getSize() > maxSize) {
			throw new IllegalArgumentException("File size exceeds 10MB limit");
		}
	}

	private String getFileExtension(String filename) {
		if (filename == null || filename.lastIndexOf('.') == -1) {
			return "";
		}
		return filename.substring(filename.lastIndexOf('.') + 1);
	}
}

