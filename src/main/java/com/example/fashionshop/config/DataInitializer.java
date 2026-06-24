package com.example.fashionshop.config;

import com.example.fashionshop.entity.User;
import com.example.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByEmail("thanhtung232004@gmail.com")) {

            User admin = User.builder()
                    .fullName("Nguyễn Thành Tùng")
                    .email("thanhtung232004@gmail.com")
                    .password(passwordEncoder.encode("NTTung.2304"))
                    .phone("0971350813")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);

            System.out.println("Default admin account created");
        }
    }
}