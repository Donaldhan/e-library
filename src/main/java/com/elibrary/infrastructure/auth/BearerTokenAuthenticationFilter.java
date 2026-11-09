package com.elibrary.infrastructure.auth;

import com.elibrary.domain.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Demo authentication adapter: an opaque bearer token maps to a seeded member.
 *
 * <p>A missing or unknown token is not rejected here; endpoints that need an identity declare
 * {@code @CurrentMember} and the argument resolver produces a 401. That keeps public catalogue
 * browsing usable without credentials while still protecting lending operations.
 */
@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final MemberRepository members;

    public BearerTokenAuthenticationFilter(MemberRepository members) {
        this.members = members;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            members.findByToken(token)
                    .map(MemberPrincipal::from)
                    .ifPresent(principal -> request.setAttribute(MemberPrincipal.REQUEST_ATTRIBUTE, principal));
        }
        filterChain.doFilter(request, response);
    }
}
