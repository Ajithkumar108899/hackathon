package com.bulletjournal.Companion.App.mapper;


import com.bulletjournal.Companion.App.dto.OriginalPasswordResponse;
import com.bulletjournal.Companion.App.dto.PasswordResponse;
import com.bulletjournal.Companion.App.dto.UserResponse;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public UserResponse toUserResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.phoneNumber(user.getPhoneNumber())
				.enabled(user.getEnabled())
				.createdAt(user.getCreatedAt())
				.build();
	}


    public void applyUpdates(User updateUser, User existingUser) {
        if (updateUser == null || existingUser == null) {
            return;
        }
        if (updateUser.getFirstName() != null) {
            existingUser.setFirstName(updateUser.getFirstName());
        }
        if (updateUser.getLastName() != null) {
            existingUser.setLastName(updateUser.getLastName());
        }
        if (updateUser.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updateUser.getPhoneNumber());
        }
        if (updateUser.getEnabled() != null) {
            existingUser.setEnabled(updateUser.getEnabled());
        }
    }

    public PasswordResponse toPasswordResponse(User user) {
        return PasswordResponse.builder()
                .email(user.getEmail())
                .hashedPassword(user.getPassword())
                .message("Password is BCrypt hashed and cannot be decoded. This is a one-way hash for security.")
                .build();
    }

    public OriginalPasswordResponse toOriginalPasswordResponse(User user, String originalPassword) {
        return OriginalPasswordResponse.builder()
                .email(user.getEmail())
                .originalPassword(originalPassword)
                .message("Original password retrieved successfully. This password was used during user registration.")
                .build();
    }
}

