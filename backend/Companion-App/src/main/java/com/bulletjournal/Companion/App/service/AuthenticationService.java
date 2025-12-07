package com.bulletjournal.Companion.App.service;


import com.bulletjournal.Companion.App.dto.AuthRequest;
import com.bulletjournal.Companion.App.dto.AuthResponse;
import com.bulletjournal.Companion.App.dto.LogoutResponse;
import com.bulletjournal.Companion.App.dto.RegisterRequest;
import com.bulletjournal.Companion.App.mapper.AuthMapper;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	
	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;	
	private final AuthMapper authMapper;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		// Check if user already exists
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
		if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
			throw new RuntimeException("Phone number already exists");
		}

		User user = authMapper.createUser(request);

		user = userRepository.save(user);

		// Generate tokens
		String accessToken = jwtService.generateToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);

		return authMapper.toAuthResponse(user, accessToken, refreshToken, jwtService);

	}

	@Transactional
	public AuthResponse authenticate(AuthRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getEmail(),
						request.getPassword()
				)
		);

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// Update last activity time (user is now online)
		user.setLastActivityAt(LocalDateTime.now());
		userRepository.save(user);

		String accessToken = jwtService.generateToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);

		return authMapper.toAuthResponse(user, accessToken, refreshToken, jwtService);
	}

	@Transactional
	public AuthResponse refreshToken(String refreshToken) {
		String userEmail = jwtService.extractUsername(refreshToken);
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!jwtService.isTokenValid(refreshToken, user)) {
			throw new RuntimeException("Invalid refresh token");
		}

		// Update last activity time (user is now online)
		user.setLastActivityAt(LocalDateTime.now());
		userRepository.save(user);

		String newAccessToken = jwtService.generateToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user);

		return authMapper.toAuthResponse(user, newAccessToken, newRefreshToken, jwtService);
	}

	public LogoutResponse logout() {
		return authMapper.toLogout();
	}
}

