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

	public OcrService() {
		this.tesseract = new Tesseract();
		// Set Tesseract data path (default is usually fine, but can be customized)
		// tesseract.setDatapath("path/to/tessdata");
		// Set language (default is English, can add more languages)
		tesseract.setLanguage("eng");
		// Configure OCR settings for better accuracy
		tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
		tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
	}

	/**
	 * Extract text from image file
	 * @param imageFile The image file to process
	 * @return Extracted text
	 */
	public String extractText(File imageFile) throws TesseractException, IOException {
		log.info("Starting OCR extraction for file: {}", imageFile.getName());
		
		// Preprocess image for better OCR results
		BufferedImage processedImage = preprocessImage(imageFile);
		
		// Perform OCR
		String extractedText = tesseract.doOCR(processedImage);
		
		log.info("OCR extraction completed. Extracted {} characters", extractedText.length());
		return extractedText.trim();
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
	 * Preprocess image to improve OCR accuracy
	 * - Convert to grayscale
	 * - Resize if needed
	 */
	private BufferedImage preprocessImage(File imageFile) throws IOException {
		BufferedImage image = ImageIO.read(imageFile);
		
		if (image == null) {
			throw new IOException("Unable to read image file: " + imageFile.getName());
		}

		// Convert to grayscale for better OCR accuracy
		BufferedImage grayscale = Scalr.apply(image, Scalr.OP_GRAYSCALE);
		
		// Resize if image is too small (minimum 300 DPI recommended for OCR)
		// But don't resize if already large enough
		int minWidth = 1200;
		int minHeight = 1200;
		
		BufferedImage processed = grayscale;
		if (grayscale.getWidth() < minWidth || grayscale.getHeight() < minHeight) {
			// Scale up if too small
			double scaleX = (double) minWidth / grayscale.getWidth();
			double scaleY = (double) minHeight / grayscale.getHeight();
			double scale = Math.max(scaleX, scaleY);
			
			int newWidth = (int) (grayscale.getWidth() * scale);
			int newHeight = (int) (grayscale.getHeight() * scale);
			
			processed = Scalr.resize(grayscale, Scalr.Method.QUALITY, newWidth, newHeight);
		}
		
		return processed;
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

