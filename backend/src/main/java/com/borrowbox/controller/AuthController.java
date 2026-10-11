package com.borrowbox.controller;

import com.borrowbox.dto.*;
import com.borrowbox.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) { return authService.register(request); }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }

    @PostMapping("/admin-login")
    public AuthResponse adminLogin(@Valid @RequestBody LoginRequest request) { return authService.adminLogin(request); }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication auth) { return authService.currentUser(auth.getName()); }

    @PutMapping("/me")
    public UserResponse updateProfile(Authentication auth, @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(auth.getName(), request);
    }
}
