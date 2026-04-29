package com.smartlogix.auth.dto;

public class CredentialCheckRequest {

    private String username;
    private String rawPassword;

    public CredentialCheckRequest() {}

    public CredentialCheckRequest(String username, String rawPassword) {
        this.username = username;
        this.rawPassword = rawPassword;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRawPassword() { return rawPassword; }
    public void setRawPassword(String rawPassword) { this.rawPassword = rawPassword; }
}
