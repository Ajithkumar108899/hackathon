package com.bulletjournal.Companion.App.repository;

import com.bulletjournal.Companion.App.model.Task;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
	
	List<Task> findByUser(User user);
	
	List<Task> findByUserId(Long userId);
	
	List<Task> findByJournalPageId(Long journalPageId);
	
	Optional<Task> findByPositionHashAndJournalPageId(String positionHash, Long journalPageId);
	
	@Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
		   "(LOWER(t.content) LIKE LOWER(CONCAT('%', :query, '%')))")
	List<Task> searchByContent(@Param("userId") Long userId, @Param("query") String query);
}

