package com.dto;

public class LoginResponse {
    private String token;
    private String name;
    private String email;
    private String role;
    private String tenant;

    public LoginResponse(String token, String name, String email, String role, String tenant) {
        this.token = token;
        this.name = name;
        this.email = email;
        this.role = role;
        this.tenant = tenant;
    }

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
