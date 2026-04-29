package com.smartlogix.auth.controller;

import com.smartlogix.auth.dto.LoginRequest;
import com.smartlogix.auth.dto.TokenResponse;
import com.smartlogix.auth.dto.ValidateTokenRequest;
import com.smartlogix.auth.dto.ValidateTokenResponse;
import com.smartlogix.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/validate")
    public ValidateTokenResponse validate(@Valid @RequestBody ValidateTokenRequest request) {
        return authService.validate(request);
    }
}
