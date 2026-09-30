package com.bank.userservice.security;

import com.bank.userservice.entity.UserStatus;
import com.bank.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {

            String role = jwtAuth.getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("");

            // Admins must be able to reactivate disabled users.
            if ("ROLE_ADMIN".equals(role)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtAuth.getToken().getClaim("userId");

            if (userId == null) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid authentication token"
                );
                return;
            }

            UserStatus status = userRepository.findById(userId)
                    .map(user -> user.getStatus())
                    .orElse(null);

            if (status != UserStatus.ACTIVE) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Your account is disabled. Please contact the administrator."
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}