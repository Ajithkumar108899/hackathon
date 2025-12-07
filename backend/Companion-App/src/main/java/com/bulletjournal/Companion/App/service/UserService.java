package com.bulletjournal.Companion.App.service;


import com.bulletjournal.Companion.App.dto.OriginalPasswordResponse;
import com.bulletjournal.Companion.App.dto.PasswordResponse;
import com.bulletjournal.Companion.App.dto.UserResponse;
import com.bulletjournal.Companion.App.mapper.UserMapper;
import com.bulletjournal.Companion.App.model.User;
import com.bulletjournal.Companion.App.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncryptionService passwordEncryptionService;


	public UserResponse getUserByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found with email: " + email));
		if (!user.isEnabled()) {
			throw new RuntimeException("User has been deleted");
		}
		return userMapper.toUserResponse(user);
	}

	public UserResponse getUserByTrueId(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));
		if (!user.isEnabled()) {
			throw new RuntimeException("User has been deleted");
		}

		return userMapper.toUserResponse(user);
	}

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream()
				.filter(User::isEnabled) // Only enabled users
				.map(userMapper::toUserResponse)
				.collect(Collectors.toList());
	}


	@Transactional
	public void deleteUser(Long id) {
		if (!userRepository.existsById(id)) {
			throw new RuntimeException("User not found with id: " + id);
		}
		userRepository.deleteById(id);
	}

	@Transactional
	public UserResponse softDeleteUser(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));
		
		if (!user.isEnabled()){
			throw new RuntimeException("User is already soft-deleted.");
		}
		
		user.setEnabled(false); // Disable the account (soft delete)
		
		user = userRepository.save(user);
		return userMapper.toUserResponse(user);
	}



	public OriginalPasswordResponse getOriginalPassword(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found with email: " + email));
		if (user.getEncryptedOriginalPassword() == null || user.getEncryptedOriginalPassword().isEmpty()) {
			throw new RuntimeException("Original password not available for this user. User may have been created before password encryption was implemented.");
		}
		String originalPassword = passwordEncryptionService.decrypt(user.getEncryptedOriginalPassword());
		return userMapper.toOriginalPasswordResponse(user, originalPassword);
	}



}

