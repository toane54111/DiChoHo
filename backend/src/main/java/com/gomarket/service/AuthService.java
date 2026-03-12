package com.gomarket.service;

import com.gomarket.dto.LoginRequest;
import com.gomarket.dto.RegisterRequest;
import com.gomarket.dto.UserResponse;
import com.gomarket.model.User;
import com.gomarket.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Sai mật khẩu");
        }

        String token = UUID.randomUUID().toString();
        return UserResponse.fromUser(user, token);
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được đăng ký");
        }

        User user = new User(
                request.getFull_name(),
                request.getPhone(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole() != null ? request.getRole() : "BUYER"
        );

        user = userRepository.save(user);
        String token = UUID.randomUUID().toString();
        return UserResponse.fromUser(user, token);
    }
}
