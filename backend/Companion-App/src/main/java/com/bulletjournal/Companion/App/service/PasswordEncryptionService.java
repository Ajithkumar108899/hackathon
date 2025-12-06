package com.bulletjournal.Companion.App.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class PasswordEncryptionService {

	@Value("${jwt.secret}")
	private String secret;

	private static final String ALGORITHM = "AES";

	private String getEncryptionSecret() {
		return secret;
	}

	public String encrypt(String password) {
		try {
			// Ensure key is 16 bytes for AES-128
			String encryptionSecret = getEncryptionSecret();
			byte[] key = encryptionSecret.getBytes(StandardCharsets.UTF_8);
			if (key.length != 16) {
				// Pad or truncate to 16 bytes
				byte[] paddedKey = new byte[16];
				System.arraycopy(key, 0, paddedKey, 0, Math.min(key.length, 16));
				key = paddedKey;
			}

			SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);

			byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (Exception e) {
			throw new RuntimeException("Error encrypting password", e);
		}
	}

	public String decrypt(String encryptedPassword) {
		try {
			// Ensure key is 16 bytes for AES-128
			String encryptionSecret = getEncryptionSecret();
			byte[] key = encryptionSecret.getBytes(StandardCharsets.UTF_8);
			if (key.length != 16) {
				// Pad or truncate to 16 bytes
				byte[] paddedKey = new byte[16];
				System.arraycopy(key, 0, paddedKey, 0, Math.min(key.length, 16));
				key = paddedKey;
			}

			SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);

			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
			return new String(decryptedBytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("Error decrypting password", e);
		}
	}
}

