package com.bulletjournal.Companion.App.controller;

import com.bulletjournal.Companion.App.dto.EmailRequest;
import com.bulletjournal.Companion.App.dto.OriginalPasswordResponse;
import com.bulletjournal.Companion.App.dto.PasswordResponse;
import com.bulletjournal.Companion.App.dto.UserResponse;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

	private final UserService userService;

	@GetMapping("/currentUser")
	@Operation(summary = "Get current user", description = "Get details of the currently authenticated user")
	public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
		UserResponse response = userService.getUserByEmail(user.getEmail());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getUserByActiveTrueId/{id}")
	@Operation(summary = "Get user by ID", description = "Retrieve user details by user ID")
	public ResponseEntity<UserResponse> getUserByTrueId(@PathVariable Long id) {
		UserResponse response = userService.getUserByTrueId(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getUserByDeleteFalseId/{id}")
	@Operation(summary = "Get soft-deleted user by ID", description = "Retrieve soft-deleted user details by user ID (enabled = false). Only returns users that are soft-deleted. Requires SUPER_ADMIN role.")
	public ResponseEntity<UserResponse> getUserByFalseId(@PathVariable Long id) {
		UserResponse response = userService.getUserByFalseId(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getAllActiveTrueUsers")
	@Operation(summary = "Get all users", description = "Retrieve all users (Super Admin only)")
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		List<UserResponse> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/getOnlineUsers")
	@Operation(
		summary = "Get all online users", 
		description = "Retrieve all users who are currently online (active within last 15 minutes). Only active users (enabled = true) are considered. Requires SUPER_ADMIN role."
	)
	public ResponseEntity<List<UserResponse>> getOnlineUsers() {
		List<UserResponse> users = userService.getOnlineUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/getAllDeleteFalseUser")
	@Operation(summary = "Get all soft-deleted users", description = "Retrieve all soft-deleted users (enabled = false). Requires SUPER_ADMIN role.")
	public ResponseEntity<List<UserResponse>> getAllFalseUser() {
		List<UserResponse> users = userService.getAllFalseUser();
		return ResponseEntity.ok(users);
	}




	@DeleteMapping("/deleteUserById/{id}")
	@Operation(summary = "Delete user (Hard Delete)", description = "Permanently delete a user by ID (Super Admin only)")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/softDeleteUserById/{id}")
	@Operation(summary = "Soft delete user", description = "Soft delete (disable) an active user by ID - sets enabled to false in database. User must be active (enabled = true) to be soft-deleted. Requires SUPER_ADMIN role.")
	public ResponseEntity<UserResponse> softDeleteUser(@PathVariable Long id) {
		UserResponse response = userService.softDeleteUser(id);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/enableUserById/{id}")
	@Operation(
		summary = "Enable user (Reactivate)", 
		description = "Enable (reactivate) a soft-deleted user by ID - sets enabled to true in database. User must be soft-deleted (enabled = false) to be enabled. Requires SUPER_ADMIN role."
	)
	public ResponseEntity<UserResponse> enableUser(@PathVariable Long id) {
		UserResponse response = userService.enableUser(id);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/getActivePasswordByEmail")
	@Operation(summary = "Get password by email", description = "Retrieves the hashed password for a user by email. Note: Passwords are BCrypt hashed (one-way) and cannot be decoded."
	)
	public ResponseEntity<PasswordResponse> getPasswordByEmail(@RequestBody EmailRequest emailRequest) {
		PasswordResponse response = userService.getUserEntityByEmail(emailRequest.getEmail());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/getDeletePasswordByEmail")
	@Operation(
		summary = "Get password by email for soft-deleted user", 
		description = "Retrieves the hashed password for a soft-deleted user (enabled = false) by email. Note: Passwords are BCrypt hashed (one-way) and cannot be decoded. Requires SUPER_ADMIN role."
	)
	public ResponseEntity<PasswordResponse> getDeletePasswordByEmail(@RequestBody EmailRequest emailRequest) {
		PasswordResponse response = userService.getDeletePasswordByEmail(emailRequest.getEmail());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/getOriginalPasswordByEmail")
	@Operation(summary = "Get original password by email", description = "Retrieves the original (decrypted) password for a user by email. Password is stored in AES encrypted format and can be decrypted. Only available for users created after password encryption was implemented."
	)
	public ResponseEntity<OriginalPasswordResponse> getOriginalPasswordByEmail(@RequestBody EmailRequest emailRequest) {
		OriginalPasswordResponse response = userService.getOriginalPassword(emailRequest.getEmail());
		return ResponseEntity.ok(response);
	}


}

