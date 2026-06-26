package com.finbridge.security;

import com.finbridge.entity.OrganizationUser;
import com.finbridge.entity.User;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        // Only authenticate when the token is valid. A missing/expired/invalid token must NOT
        // short-circuit the request here — otherwise a stale token in the browser would break
        // public endpoints like /auth/login. We simply leave the security context unauthenticated
        // and let Spring Security's authorization rules decide (public routes proceed, protected
        // routes are rejected with 401/403).
        if (jwtService.isTokenValid(token)) {
            if (jwtService.isB2BToken(token)) {
                authenticateB2B(token);
            } else {
                authenticateCrm(token);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** CRM/staff/client principal — loaded from the users table. */
    private void authenticateCrm(String token) {
        String userId = jwtService.extractUserId(token);
        Optional<User> userOpt = userRepository.findById(UUID.fromString(userId));
        if (userOpt.isPresent() && userOpt.get().isActive()) {
            User user = userOpt.get();
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    /** B2B organization-portal principal — loaded from the organization_users table. */
    private void authenticateB2B(String token) {
        String orgUserId = jwtService.extractUserId(token);
        Optional<OrganizationUser> orgUserOpt = organizationUserRepository.findById(UUID.fromString(orgUserId));
        if (orgUserOpt.isPresent() && orgUserOpt.get().isActive()) {
            OrganizationUser orgUser = orgUserOpt.get();
            // Grant both a generic B2B authority and the org user's specific role
            // (e.g. ROLE_COMPANY_ADMIN) so endpoints can tighten later if needed.
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_ORG_USER"),
                    new SimpleGrantedAuthority("ROLE_" + orgUser.getRole().toUpperCase()));
            var auth = new UsernamePasswordAuthenticationToken(orgUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }
}
