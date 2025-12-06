package com.bulletjournal.Companion.App.repository;

import com.bulletjournal.Companion.App.model.JournalPage;
import com.bulletjournal.Companion.App.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalPageRepository extends JpaRepository<JournalPage, Long> {
	
	List<JournalPage> findByUser(User user);
	
	List<JournalPage> findByUserId(Long userId);
	
	Optional<JournalPage> findByIdAndUserId(Long id, Long userId);
	
	List<JournalPage> findByThreadId(String threadId);
}

