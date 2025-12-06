package com.bulletjournal.Companion.App.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OriginalPasswordResponse {
	private String email;
	private String originalPassword;
	private String message;
}

