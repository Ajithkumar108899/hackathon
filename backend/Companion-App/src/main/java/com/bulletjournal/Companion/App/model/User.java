package com.bulletjournal.Companion.App.model;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

    // Explicit setter for encryptedOriginalPassword
    @Column(name = "encrypted_original_password")
	private String encryptedOriginalPassword; // AES encrypted original password for retrieval

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@Column(unique = true)
	private String phoneNumber;

    // Explicit setter for Boolean enabled field
    @Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;


	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

    // Explicit getter for lastActivityAt
    @Column(name = "last_activity_at")
	private LocalDateTime lastActivityAt; // Track last activity time for online status

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// -------------------------
	// UserDetails interface methods
	// -------------------------

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Return empty authorities - no role-based access control
		return Collections.emptyList();
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}


	@Override
	public boolean isEnabled() {
		return enabled != null && enabled;
	}

}

