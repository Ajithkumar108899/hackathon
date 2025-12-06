package com.bulletjournal.Companion.App.service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private Long expiration;

	@Value("${jwt.refresh-expiration}")
	private Long refreshExpiration;

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		// Extract role from authorities
		if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
			String role = userDetails.getAuthorities().iterator().next().getAuthority();
			// Remove ROLE_ prefix if present, store as "role" claim
			if (role.startsWith("ROLE_")) {
				role = role.substring(5);
			}
			claims.put("role", role);
		}
		return generateToken(claims, userDetails);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		// Ensure role is in claims
		if (!extraClaims.containsKey("role") && userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
			String role = userDetails.getAuthorities().iterator().next().getAuthority();
			if (role.startsWith("ROLE_")) {
				role = role.substring(5);
			}
			extraClaims.put("role", role);
		}
		return buildToken(extraClaims, userDetails, expiration);
	}

	public String generateRefreshToken(UserDetails userDetails) {
		return buildToken(new HashMap<>(), userDetails, refreshExpiration);
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		return Jwts.builder()
				.claims(extraClaims)
				.subject(userDetails.getUsername())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSigningKey())
				.compact();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		if (userDetails != null) {
			// Validate with UserDetails (for user-service)
			final String username = extractUsername(token);
			return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
		} else {
			// Just check if token is not expired (for other microservices)
			return !isTokenExpired(token);
		}
	}

	/**
	 * Validate token without UserDetails (for microservices without user data)
	 * Validates both signature and expiration
	 */
	public boolean isTokenValid(String token) {
		try {
			// First validate signature by parsing the token
			extractAllClaims(token);
			// Then check expiration
			return !isTokenExpired(token);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = secret.getBytes();
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public Long getExpiration() {
		return expiration;
	}
}

