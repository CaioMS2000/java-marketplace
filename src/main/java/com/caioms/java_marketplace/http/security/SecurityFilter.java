package com.caioms.java_marketplace.http.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	private final JWTProvider jwtProvider;
	private final String rolesClaim;

	public SecurityFilter(JWTProvider jwtProvider,
	        @Value("${security.token.roles-claim}") String rolesClaim) {
		this.jwtProvider = jwtProvider;
		this.rolesClaim = rolesClaim;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
	        FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null) {
			var tokenOpt = jwtProvider.validate(header);
			if (tokenOpt.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			authenticate(tokenOpt.get());
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(DecodedJWT token) {
		var claim = token.getClaim(rolesClaim);
		var roles = (claim.isMissing() || claim.isNull())
		        ? List.<String>of()
		        : claim.asList(String.class);

		var grants = roles.stream()
		        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())).toList();

		var auth = new UsernamePasswordAuthenticationToken(token.getSubject(), null, grants);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
