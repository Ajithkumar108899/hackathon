package com.bulletjournal.Companion.App.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class OcrService {

	private final ITesseract tesseract;

	public OcrService(@org.springframework.beans.factory.annotation.Value("${ocr.tesseract.datapath:}") String tessdataPath) {
		this.tesseract = new Tesseract();
		
		// Set Tesseract data path
		String finalPath = null;
		if (tessdataPath != null && !tessdataPath.isEmpty()) {
			// Normalize path for Windows
			finalPath = normalizePath(tessdataPath);
		} else {
			// Try common default paths
			String[] defaultPaths = {
				"C:\\Program Files\\Tesseract-OCR\\tessdata",  // Windows default
				"C:\\Tesseract-OCR\\tessdata",                 // Windows alternative
				"/usr/share/tesseract-ocr/5/tessdata",        // Linux (Ubuntu/Debian)
				"/usr/share/tesseract-ocr/4.00/tessdata",      // Linux older version
				"/usr/local/share/tessdata",                   // Mac/Linux alternative
				"/opt/homebrew/share/tessdata"                 // Mac Homebrew
			};
			
			boolean pathSet = false;
			for (String path : defaultPaths) {
				java.io.File pathFile = new java.io.File(path);
				if (pathFile.exists() && pathFile.isDirectory()) {
					java.io.File engFile = new java.io.File(pathFile, "eng.traineddata");
					if (engFile.exists()) {
						finalPath = path;
						log.info("Tesseract data path auto-detected: {}", path);
						pathSet = true;
						break;
					}
				}
			}
			
			if (!pathSet) {
				log.warn("Tesseract data path not found. Please set 'ocr.tesseract.datapath' in application.properties");
				log.warn("Common locations:");
				log.warn("  Windows: C:\\Program Files\\Tesseract-OCR\\tessdata");
				log.warn("  Linux: /usr/share/tesseract-ocr/5/tessdata");
				log.warn("  Mac: /usr/local/share/tessdata");
			}
		}
		
		// Set the datapath and TESSDATA_PREFIX environment variable
		if (finalPath != null) {
			// Verify the path exists
			java.io.File pathFile = new java.io.File(finalPath);
			if (!pathFile.exists() || !pathFile.isDirectory()) {
				log.warn("Tesseract data path does not exist: {}", finalPath);
			} else {
				// Check if eng.traineddata exists
				java.io.File engFile = new java.io.File(pathFile, "eng.traineddata");
				if (!engFile.exists()) {
					log.warn("eng.traineddata not found in: {}", finalPath);
				} else {
					log.info("Tesseract data path verified. eng.traineddata found at: {}", engFile.getAbsolutePath());
				}
			}
			
			// Set datapath using both methods
			tesseract.setDatapath(finalPath);
			
			// Also set TESSDATA_PREFIX environment variable (helps with some Tesseract versions)
			try {
				System.setProperty("TESSDATA_PREFIX", finalPath);
				log.info("TESSDATA_PREFIX environment variable set to: {}", finalPath);
			} catch (Exception e) {
				log.warn("Failed to set TESSDATA_PREFIX: {}", e.getMessage());
			}
		}
		
		// Set language
		String language = System.getProperty("ocr.tesseract.language", "eng");
		tesseract.setLanguage(language);
		log.info("Tesseract language set to: {}", language);
		
		// Configure OCR settings for better accuracy with handwritten text
		// Try different PSM modes for handwritten text:
		// PSM 11 = Sparse text (good for lists)
		// PSM 12 = Sparse text with OSD (orientation and script detection)
		// PSM 6 = Uniform block (default for handwritten)
		tesseract.setPageSegMode(11); // Sparse text - better for lists and bullet points
		tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
		
		// Remove character whitelist - it might be too restrictive for handwritten text
		// Let Tesseract recognize all characters naturally
		// tesseract.setTessVariable("tessedit_char_whitelist", "..."); // Commented out
		
		// Additional settings for better handwritten recognition
		tesseract.setTessVariable("tessedit_pageseg_mode", "11");
		tesseract.setTessVariable("classify_bln_numeric_mode", "1"); // Better number recognition
	}
	
	/**
	 * Normalize file path for the current OS
	 */
	private String normalizePath(String path) {
		if (path == null || path.isEmpty()) {
			return path;
		}
		// Convert to absolute path and normalize
		java.io.File file = new java.io.File(path);
		return file.getAbsolutePath();
	}

	/**
	 * Extract text from image file
	 * @param imageFile The image file to process
	 * @return Extracted text (empty string if OCR fails or is not available)
	 */
	public String extractText(File imageFile) throws TesseractException, IOException {
		try {
			log.info("Starting OCR extraction for file: {}", imageFile.getName());
			
			// Preprocess image for better OCR results
			BufferedImage processedImage = preprocessImage(imageFile);
			
			// Perform OCR
			String extractedText = tesseract.doOCR(processedImage);
			
			if (extractedText != null && !extractedText.trim().isEmpty()) {
				log.info("OCR extraction completed. Extracted {} characters", extractedText.length());
				return extractedText.trim();
			} else {
				log.warn("OCR returned empty text");
				return "";
			}
		} catch (Error e) {
			// Handle native library errors (Tesseract not installed)
			log.error("Tesseract OCR is not available or not properly installed: {}", e.getMessage());
			log.warn("Please install Tesseract OCR to enable text extraction. Application will continue without OCR.");
			return ""; // Return empty string instead of throwing exception
		} catch (TesseractException e) {
			log.error("Tesseract OCR error: {}", e.getMessage());
			return ""; // Return empty string instead of throwing exception
		}
	}

	/**
	 * Extract text from image file path
	 */
	public String extractText(String imagePath) throws TesseractException, IOException {
		File imageFile = new File(imagePath);
		if (!imageFile.exists()) {
			throw new IOException("Image file not found: " + imagePath);
		}
		return extractText(imageFile);
	}

	/**
	 * Preprocess image to improve OCR accuracy for handwritten text
	 * - Convert to grayscale
	 * - Enhance contrast
	 * - Apply thresholding (binarization)
	 * - Resize if needed
	 */
	private BufferedImage preprocessImage(File imageFile) throws IOException {
		BufferedImage image = ImageIO.read(imageFile);
		
		if (image == null) {
			throw new IOException("Unable to read image file: " + imageFile.getName());
		}

		// Step 1: Convert to grayscale
		BufferedImage grayscale = Scalr.apply(image, Scalr.OP_GRAYSCALE);
		
		// Step 2: Enhance contrast using RescaleOp (more aggressive for handwritten)
		java.awt.image.RescaleOp rescaleOp = new java.awt.image.RescaleOp(1.5f, -15.0f, null);
		BufferedImage contrast = rescaleOp.filter(grayscale, null);
		
		// Step 3: Apply thresholding (binarization) for better text recognition
		// Use regular thresholding (adaptive is too slow for large images)
		BufferedImage binary = applyThreshold(contrast);
		
		// Step 4: Resize if image is too small (minimum 300 DPI recommended for OCR)
		int minWidth = 1500; // Increased for better handwritten text recognition
		int minHeight = 1500;
		
		BufferedImage processed = binary;
		if (binary.getWidth() < minWidth || binary.getHeight() < minHeight) {
			// Scale up if too small
			double scaleX = (double) minWidth / binary.getWidth();
			double scaleY = (double) minHeight / binary.getHeight();
			double scale = Math.max(scaleX, scaleY);
			
			int newWidth = (int) (binary.getWidth() * scale);
			int newHeight = (int) (binary.getHeight() * scale);
			
			processed = Scalr.resize(binary, Scalr.Method.QUALITY, newWidth, newHeight);
		}
		
		return processed;
	}
	
	/**
	 * Apply thresholding to create binary (black/white) image
	 * This helps OCR recognize handwritten text better
	 */
	private BufferedImage applyThreshold(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		
		// Threshold value (adjust between 0-255, lower = more sensitive)
		int threshold = 140; // Slightly higher for better text detection
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = image.getRGB(x, y);
				int gray = (int) (((rgb >> 16) & 0xFF) * 0.299 + 
								  ((rgb >> 8) & 0xFF) * 0.587 + 
								  (rgb & 0xFF) * 0.114);
				
				// Convert to binary (black or white)
				int newRgb = (gray < threshold) ? 0x000000 : 0xFFFFFF;
				binary.setRGB(x, y, newRgb);
			}
		}
		
		return binary;
	}
	
	/**
	 * Apply adaptive thresholding for better results with varying lighting
	 * This is better for handwritten text with shadows or uneven lighting
	 */
	private BufferedImage applyAdaptiveThreshold(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		
		// Block size for adaptive thresholding (should be odd)
		int blockSize = 15;
		int c = 5; // Constant subtracted from mean
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Calculate local mean in blockSize x blockSize neighborhood
				int sum = 0;
				int count = 0;
				
				for (int dy = -blockSize/2; dy <= blockSize/2; dy++) {
					for (int dx = -blockSize/2; dx <= blockSize/2; dx++) {
						int nx = x + dx;
						int ny = y + dy;
						
						if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
							int rgb = image.getRGB(nx, ny);
							int gray = (int) (((rgb >> 16) & 0xFF) * 0.299 + 
											  ((rgb >> 8) & 0xFF) * 0.587 + 
											  (rgb & 0xFF) * 0.114);
							sum += gray;
							count++;
						}
					}
				}
				
				int mean = sum / count;
				int threshold = mean - c;
				
				// Get current pixel value
				int rgb = image.getRGB(x, y);
				int gray = (int) (((rgb >> 16) & 0xFF) * 0.299 + 
								  ((rgb >> 8) & 0xFF) * 0.587 + 
								  (rgb & 0xFF) * 0.114);
				
				// Convert to binary
				int newRgb = (gray < threshold) ? 0x000000 : 0xFFFFFF;
				binary.setRGB(x, y, newRgb);
			}
		}
		
		return binary;
	}

	/**
	 * Check if Tesseract is properly configured
	 */
	public boolean isOcrAvailable() {
		try {
			// Try to get Tesseract version
			String version = tesseract.getClass().getPackage().getImplementationVersion();
			log.info("Tesseract OCR is available. Version: {}", version);
			return true;
		} catch (Exception e) {
			log.warn("Tesseract OCR may not be properly configured: {}", e.getMessage());
			return false;
		}
	}
}

