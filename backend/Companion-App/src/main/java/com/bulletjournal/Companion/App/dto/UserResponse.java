package com.bulletjournal.Companion.App.dto;


import com.bulletjournal.Companion.App.roles.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private Role role;
	private Boolean enabled;
	private LocalDateTime createdAt;
}

