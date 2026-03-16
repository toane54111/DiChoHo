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

    // Shopper fields
    private Double latitude;
    private Double longitude;
    private Boolean is_online;
    private String bio;
    private Double rating;
    private Integer total_orders;
    private String vehicle_type;

    public static UserResponse fromUser(User user, String token) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.full_name = user.getFullName();
        response.phone = user.getPhone();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.avatar_url = user.getAvatarUrl();
        response.token = token;
        response.latitude = user.getLatitude();
        response.longitude = user.getLongitude();
        response.is_online = user.getIsOnline();
        response.bio = user.getBio();
        response.rating = user.getRating();
        response.total_orders = user.getTotalOrders();
        response.vehicle_type = user.getVehicleType();
        return response;
    }

    public Long getId() { return id; }
    public String getFull_name() { return full_name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatar_url() { return avatar_url; }
    public String getToken() { return token; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Boolean getIs_online() { return is_online; }
    public String getBio() { return bio; }
    public Double getRating() { return rating; }
    public Integer getTotal_orders() { return total_orders; }
    public String getVehicle_type() { return vehicle_type; }
}
