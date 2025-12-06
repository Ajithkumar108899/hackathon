package com.bulletjournal.Companion.App.repository;

import com.bulletjournal.Companion.App.model.Event;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
	
	List<Event> findByUser(User user);
	
	List<Event> findByUserId(Long userId);
	
	List<Event> findByJournalPageId(Long journalPageId);
	
	Optional<Event> findByPositionHashAndJournalPageId(String positionHash, Long journalPageId);
	
	List<Event> findByUserIdAndEventDate(Long userId, LocalDate eventDate);
	
	@Query("SELECT e FROM Event e WHERE e.user.id = :userId AND " +
		   "(LOWER(e.content) LIKE LOWER(CONCAT('%', :query, '%')))")
	List<Event> searchByContent(@Param("userId") Long userId, @Param("query") String query);
}

