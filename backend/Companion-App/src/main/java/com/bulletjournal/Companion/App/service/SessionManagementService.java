package com.bulletjournal.Companion.App.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SessionManagementService {

	@Value("${session.timeout}")
	private int timeout;

	@Value("${session.max-concurrent}")
	private int maxConcurrent;

	// Store active sessions: userId -> Map<sessionId, lastActivityTime>
	private final Map<String, Map<String, LocalDateTime>> userSessions = new ConcurrentHashMap<>();

	/**
	 * Register a new session for a user
	 */
	public void registerSession(String userId, String sessionId) {
		userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
				.put(sessionId, LocalDateTime.now());

		// Check if user has exceeded max concurrent sessions
		Map<String, LocalDateTime> sessions = userSessions.get(userId);
		if (sessions.size() > maxConcurrent) {
			// Remove oldest session
			String oldestSession = sessions.entrySet().stream()
					.min(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey)
					.orElse(null);

			if (oldestSession != null) {
				sessions.remove(oldestSession);
				log.info("Removed oldest session {} for user {} due to max concurrent limit", oldestSession, userId);
			}
		}

		log.debug("Registered session {} for user {}. Total sessions: {}", sessionId, userId, sessions.size());
	}

	/**
	 * Update session activity time
	 */
	public void updateSessionActivity(String userId, String sessionId) {
		Map<String, LocalDateTime> sessions = userSessions.get(userId);
		if (sessions != null && sessions.containsKey(sessionId)) {
			sessions.put(sessionId, LocalDateTime.now());
		}
	}

	/**
	 * Remove a session
	 */
	public void removeSession(String userId, String sessionId) {
		Map<String, LocalDateTime> sessions = userSessions.get(userId);
		if (sessions != null) {
			sessions.remove(sessionId);
			if (sessions.isEmpty()) {
				userSessions.remove(userId);
			}
			log.debug("Removed session {} for user {}", sessionId, userId);
		}
	}

	/**
	 * Get active session count for a user
	 */
	public int getActiveSessionCount(String userId) {
		Map<String, LocalDateTime> sessions = userSessions.get(userId);
		return sessions != null ? sessions.size() : 0;
	}

	/**
	 * Check if session is valid (not expired)
	 */
	public boolean isSessionValid(String userId, String sessionId) {
		Map<String, LocalDateTime> sessions = userSessions.get(userId);
		if (sessions == null || !sessions.containsKey(sessionId)) {
			return false;
		}

		LocalDateTime lastActivity = sessions.get(sessionId);
		LocalDateTime expiryTime = lastActivity.plusSeconds(timeout);

		if (LocalDateTime.now().isAfter(expiryTime)) {
			// Session expired
			sessions.remove(sessionId);
			if (sessions.isEmpty()) {
				userSessions.remove(userId);
			}
			return false;
		}

		return true;
	}

	/**
	 * Clean up expired sessions for all users
	 */
	public void cleanupExpiredSessions() {
		LocalDateTime now = LocalDateTime.now();
		userSessions.forEach((userId, sessions) -> {
			sessions.entrySet().removeIf(entry -> {
				LocalDateTime expiryTime = entry.getValue().plusSeconds(timeout);
				boolean expired = now.isAfter(expiryTime);
				if (expired) {
					log.debug("Cleaning up expired session {} for user {}", entry.getKey(), userId);
				}
				return expired;
			});

			if (sessions.isEmpty()) {
				userSessions.remove(userId);
			}
		});
	}

	/**
	 * Get total active sessions across all users
	 */
	public int getTotalActiveSessions() {
		return userSessions.values().stream()
				.mapToInt(Map::size)
				.sum();
	}
}

