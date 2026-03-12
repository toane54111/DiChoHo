package com.gomarket.dto;

import com.gomarket.model.User;

public class UserResponse {
    private Long id;
    private String full_name;
    private String phone;
    private String email;
    private String role;
    private String avatar_url;
    private String token;

    public static UserResponse fromUser(User user, String token) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.full_name = user.getFullName();
        response.phone = user.getPhone();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.avatar_url = user.getAvatarUrl();
        response.token = token;
        return response;
    }

    public Long getId() { return id; }
    public String getFull_name() { return full_name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatar_url() { return avatar_url; }
    public String getToken() { return token; }
}
