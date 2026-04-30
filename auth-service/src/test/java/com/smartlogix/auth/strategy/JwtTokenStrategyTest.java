package com.smartlogix.auth.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenStrategyTest {

    private JwtTokenStrategy tokenStrategy;

    @BeforeEach
    void setUp() {
        String secret = "SmartLogixSuperSecretKeyForJWTTokenGeneration2024ChangeInProduction";
        tokenStrategy = new JwtTokenStrategy(secret, 3600000L);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = tokenStrategy.generateToken("alice", 1L);
        assertThat(token).isNotBlank();
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = tokenStrategy.generateToken("alice", 1L);
        assertThat(tokenStrategy.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        assertThat(tokenStrategy.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = tokenStrategy.generateToken("alice", 1L);
        assertThat(tokenStrategy.extractUsername(token)).isEqualTo("alice");
    }
}
