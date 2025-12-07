package com.bulletjournal.Companion.App.controller;


import com.bulletjournal.Companion.App.dto.AuthRequest;
import com.bulletjournal.Companion.App.dto.AuthResponse;
import com.bulletjournal.Companion.App.dto.LogoutResponse;
import com.bulletjournal.Companion.App.dto.RegisterRequest;
import com.bulletjournal.Companion.App.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user registration, login, and token management")
public class AuthController {

	private final AuthenticationService authenticationService;

	@PostMapping("/create")
	@Operation(summary = "Register new user", description = "Create a new user account")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authenticationService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	@Operation(summary = "User login", description = "Authenticate user and get access token")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
		AuthResponse response = authenticationService.authenticate(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh token", description = "Get new access token using refresh token")
	public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
		if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
			refreshToken = refreshToken.substring(7);
		}
		AuthResponse response = authenticationService.refreshToken(refreshToken);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	@Operation(summary = "User logout", description = "Logout user - client should remove tokens from storage")
	public ResponseEntity<LogoutResponse> logout() {
		LogoutResponse response = authenticationService.logout();
		return ResponseEntity.ok(response);
	}
}

