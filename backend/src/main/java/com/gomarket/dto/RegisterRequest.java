package com.gomarket.dto;

public class RegisterRequest {
    private String full_name;
    private String phone;
    private String email;
    private String password;
    private String role;

    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
