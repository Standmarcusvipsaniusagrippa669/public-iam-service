package com.valiantech.core.iam.security;

import com.valiantech.core.iam.auth.service.JwtService;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdStr = claims.getSubject();
        if (userIdStr == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> userOpt = userRepository.findById(UUID.fromString(userIdStr));
        if (userOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userOpt.get();

        String role = claims.get("role", String.class);
        String companyId = claims.get("companyId", String.class);
        String fullName = claims.get("fullName", String.class);
        String status = claims.get("status", String.class);
        Boolean emailValidated = claims.get("emailValidated", Boolean.class);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        // Puedes añadir roles y authorities aquí si lo requieres
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user, null, authorities);

        // Extrae detalles estándar
        WebAuthenticationDetails standardDetails =
                new WebAuthenticationDetailsSource().buildDetails(request);

        Map<String, Object> details = new HashMap<>();
        details.put("email", user.getEmail());
        details.put("companyId", companyId);
        details.put("role", role);
        details.put("ipAddress", standardDetails.getRemoteAddress());
        details.put("sessionId", standardDetails.getSessionId());
        details.put("fullName", fullName);
        details.put("emailValidated", emailValidated);
        details.put("status", status);

        authentication.setDetails(details);

        // Asigna el usuario autenticado al contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
