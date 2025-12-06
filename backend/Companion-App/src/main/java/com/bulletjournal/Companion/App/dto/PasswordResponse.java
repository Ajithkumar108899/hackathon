package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResponse {
	private String email;
	private String hashedPassword; // BCrypt hashed password (cannot be decoded - one-way hash)
	private String message; // Warning message about password security
}

