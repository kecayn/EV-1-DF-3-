package com.smartlogix.auth.service;

import com.smartlogix.auth.client.UsersServiceClient;
import com.smartlogix.auth.dto.CredentialCheckResponse;
import com.smartlogix.auth.dto.LoginRequest;
import com.smartlogix.auth.dto.TokenResponse;
import com.smartlogix.auth.dto.ValidateTokenRequest;
import com.smartlogix.auth.dto.ValidateTokenResponse;
import com.smartlogix.auth.strategy.TokenStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsersServiceClient usersServiceClient;
    private final TokenStrategy tokenStrategy;
    private final long expirationMs;

    public AuthService(
            UsersServiceClient usersServiceClient,
            TokenStrategy tokenStrategy,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.usersServiceClient = usersServiceClient;
        this.tokenStrategy = tokenStrategy;
        this.expirationMs = expirationMs;
    }

    public TokenResponse login(LoginRequest request) {
        long start = System.currentTimeMillis();
        log.info("Login attempt for username={}", request.getUsername());

        CredentialCheckResponse check = usersServiceClient.checkCredentials(
                request.getUsername(), request.getPassword());

        long elapsed = System.currentTimeMillis() - start;

        if (check == null || !check.isValid()) {
            log.warn("Login failed for username={} elapsed={}ms", request.getUsername(), elapsed);
            throw new com.smartlogix.auth.exception.InvalidCredentialsException("Invalid username or password");
        }

        String token = tokenStrategy.generateToken(check.getUsername(), check.getUserId());
        log.info("Login successful for username={} elapsed={}ms", request.getUsername(), elapsed);
        return new TokenResponse(token, expirationMs / 1000);
    }

    public ValidateTokenResponse validate(ValidateTokenRequest request) {
        boolean valid = tokenStrategy.validateToken(request.getToken());
        if (!valid) {
            return new ValidateTokenResponse(false, null, "Token is invalid or expired");
        }
        String username = tokenStrategy.extractUsername(request.getToken());
        return new ValidateTokenResponse(true, username, "Token is valid");
    }
}
