package com.smartlogix.users.dto;

import jakarta.validation.constraints.NotBlank;

public class CredentialCheckRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String rawPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRawPassword() { return rawPassword; }
    public void setRawPassword(String rawPassword) { this.rawPassword = rawPassword; }
}
