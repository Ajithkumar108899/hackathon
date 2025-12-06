package com.bulletjournal.Companion.App.filter;


import com.bulletjournal.Companion.App.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	@Autowired(required = false)
	private UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");
		final String jwt;
		String userEmail = null; // Initialize to null

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		jwt = authHeader.substring(7);
		logger.debug("JWT token extracted, length: " + jwt.length());

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			logger.debug("SecurityContext is null, attempting JWT authentication");
			boolean authenticated = false;

			// Mode 1: With UserDetailsService (for user-service)
			if (userDetailsService != null) {
				logger.debug("Using UserDetailsService mode");
				try {
					userEmail = jwtService.extractUsername(jwt);
					UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
					if (jwtService.isTokenValid(jwt, userDetails)) {
						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
								userDetails,
								null,
								userDetails.getAuthorities()
						);
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
						authenticated = true;
						logger.info("JWT authentication successful via UserDetailsService for user: " + userEmail);
					}
				} catch (Exception e) {
					logger.debug("UserDetailsService lookup failed: " + e.getMessage() + ", falling back to token-only validation");
				}
			}

			// Mode 2: Without UserDetailsService or if Mode 1 failed (for other microservices)
			if (!authenticated) {
				// Just validate token and extract roles from claims
				logger.debug("Using token-only validation mode");
				try {
					// First validate token (this will check signature and expiration)
					logger.debug("Validating JWT token...");
					if (jwtService.isTokenValid(jwt)) {
						logger.debug("JWT token is valid, extracting claims...");
						// Extract username and claims
						userEmail = jwtService.extractUsername(jwt);
						logger.debug("Extracted username: " + userEmail);
						Claims claims = jwtService.extractClaim(jwt, claims1 -> claims1);
						Collection<? extends GrantedAuthority> authorities = extractAuthorities(claims);

						logger.debug("Extracted authorities: " + authorities);

						if (authorities.isEmpty()) {
							logger.warn("No authorities found in JWT token for user: " + userEmail);
						}

						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
								userEmail,
								null,
								authorities
						);
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
						logger.info("JWT authentication successful for user: " + userEmail + " with roles: " + authorities);
					} else {
						logger.warn("JWT token validation failed - token is invalid or expired");
					}
				} catch (Exception e) {
					logger.error("JWT validation failed, error: " + e.getMessage(), e);
					logger.error("Exception details: ", e);
				}
			}
		}
		filterChain.doFilter(request, response);
	}

	private Collection<? extends GrantedAuthority> extractAuthorities(Claims claims) {
		List<GrantedAuthority> authorities = new ArrayList<>();

		// Extract role from token claims
		Object roleObj = claims.get("role");
		if (roleObj != null) {
			String role = roleObj.toString();
			// Ensure role has ROLE_ prefix
			if (!role.startsWith("ROLE_")) {
				role = "ROLE_" + role;
			}
			authorities.add(new SimpleGrantedAuthority(role));
		}

		// Extract authorities if present
		Object authoritiesObj = claims.get("authorities");
		if (authoritiesObj instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> authList = (List<String>) authoritiesObj;
			for (String auth : authList) {
				if (!auth.startsWith("ROLE_")) {
					auth = "ROLE_" + auth;
				}
				authorities.add(new SimpleGrantedAuthority(auth));
			}
		}

		return authorities;
	}
}

