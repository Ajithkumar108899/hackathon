package com.bulletjournal.Companion.App.repository;

import com.bulletjournal.Companion.App.model.Note;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
	
	List<Note> findByUser(User user);
	
	List<Note> findByUserId(Long userId);
	
	List<Note> findByJournalPageId(Long journalPageId);
	
	Optional<Note> findByPositionHashAndJournalPageId(String positionHash, Long journalPageId);
	
	@Query("SELECT n FROM Note n WHERE n.user.id = :userId AND " +
		   "(LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))")
	List<Note> searchByContent(@Param("userId") Long userId, @Param("query") String query);
}

