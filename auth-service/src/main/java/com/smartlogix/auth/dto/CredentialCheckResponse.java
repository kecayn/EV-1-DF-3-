package com.smartlogix.auth.dto;

public class CredentialCheckResponse {

    private boolean valid;
    private Long userId;
    private String username;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
