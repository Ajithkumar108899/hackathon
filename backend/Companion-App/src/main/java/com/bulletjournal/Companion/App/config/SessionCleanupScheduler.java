package com.bulletjournal.Companion.App.config;


import com.bulletjournal.Companion.App.service.SessionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionCleanupScheduler {

	private final SessionManagementService sessionManagementService;

	/**
	 * Clean up expired sessions every 5 minutes
	 */
	@Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
	public void cleanupExpiredSessions() {
		log.debug("Starting scheduled cleanup of expired sessions");
		sessionManagementService.cleanupExpiredSessions();
		log.debug("Completed scheduled cleanup of expired sessions");
	}
}

