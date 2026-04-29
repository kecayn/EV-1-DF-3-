package com.smartlogix.auth.strategy;

public interface TokenStrategy {

    String generateToken(String username, Long userId);

    boolean validateToken(String token);

    String extractUsername(String token);
}
