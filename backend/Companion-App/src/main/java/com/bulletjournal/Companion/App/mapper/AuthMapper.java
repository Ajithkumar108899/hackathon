package com.bulletjournal.Companion.App.mapper;


import com.bulletjournal.Companion.App.dto.AuthResponse;
import com.bulletjournal.Companion.App.dto.LogoutResponse;
import com.bulletjournal.Companion.App.dto.RegisterRequest;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.roles.Role;
import com.bulletjournal.Companion.App.service.JwtService;
import com.bulletjournal.Companion.App.service.PasswordEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncryptionService passwordEncryptionService;

    public User createUser(RegisterRequest request, Role role) {
        String originalPassword = request.getPassword();
        // Store BCrypt hashed password for authentication
        String hashedPassword = passwordEncoder.encode(originalPassword);
        // Store AES encrypted original password for retrieval
        String encryptedOriginalPassword = passwordEncryptionService.encrypt(originalPassword);
        
        return User.builder()
                .email(request.getEmail())
				.password(hashedPassword)
				.encryptedOriginalPassword(encryptedOriginalPassword)
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.phoneNumber(request.getPhoneNumber())
				.role(role)
				.enabled(true)

				.build();
    }

    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, JwtService jwtService) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    public LogoutResponse toLogout() {
        return LogoutResponse.builder()
                .message("Logged out successfully")
                .success(true)
                .build();
    }
}
