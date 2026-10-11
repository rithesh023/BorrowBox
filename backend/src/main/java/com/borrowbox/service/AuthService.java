package com.borrowbox.service;

import com.borrowbox.dto.*;
import com.borrowbox.entity.AppUser;
import com.borrowbox.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.users = users; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder; this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        AppUser user = users.save(new AppUser(request.name().trim(), email,
                passwordEncoder.encode(request.password()), request.phone() == null ? null : request.phone().trim()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        return authenticate(request, false);
    }

    @Transactional(readOnly = true)
    public AuthResponse adminLogin(LoginRequest request) {
        return authenticate(request, true);
    }

    private AuthResponse authenticate(LoginRequest request, boolean adminSession) {
        String email = request.email().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            AppUser user = users.findByEmail(email).orElseThrow();
            if (adminSession && user.getRole() != com.borrowbox.entity.UserRole.ADMIN)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account does not have administrator access");
            Instant now = Instant.now();
            Instant expires = now.plus(expirationMinutes, ChronoUnit.MINUTES);
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer("borrowbox-api").issuedAt(now).expiresAt(expires)
                    .subject(user.getEmail()).claim("roles", List.of("ROLE_" + user.getRole().name()))
                    .claim("admin_session", adminSession).build();
            JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
            return new AuthResponse(token, "Bearer", expires, UserResponse.from(user));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return users.findByEmail(email).map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        AppUser user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.updateProfile(request.name().trim(), request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return UserResponse.from(user);
    }
}
