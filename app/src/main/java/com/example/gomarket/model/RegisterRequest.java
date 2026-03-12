package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    public RegisterRequest(String fullName, String phone, String email, String password, String role) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}
