package com.bulletjournal.Companion.App.repository;

import com.bulletjournal.Companion.App.model.Emotion;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionRepository extends JpaRepository<Emotion, Long> {
	
	List<Emotion> findByUser(User user);
	
	List<Emotion> findByUserId(Long userId);
	
	List<Emotion> findByJournalPageId(Long journalPageId);
	
	Optional<Emotion> findByPositionHashAndJournalPageId(String positionHash, Long journalPageId);
	
	@Query("SELECT e FROM Emotion e WHERE e.user.id = :userId AND " +
		   "(LOWER(e.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
		   "LOWER(e.emotionType) LIKE LOWER(CONCAT('%', :query, '%')))")
	List<Emotion> searchByContent(@Param("userId") Long userId, @Param("query") String query);
}

