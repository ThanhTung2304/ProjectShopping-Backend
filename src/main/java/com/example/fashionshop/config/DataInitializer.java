package com.example.fashionshop.config;

import com.example.fashionshop.entity.User;
import com.example.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        String adminEmail = System.getenv("ADMIN_BOOTSTRAP_EMAIL");
        String adminPassword = System.getenv("ADMIN_BOOTSTRAP_PASSWORD");

        // Chỉ tạo admin nếu biến môi trường được set tường minh
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        // Nếu không cung cấp password, tự sinh ngẫu nhiên và in ra log 1 lần
        boolean generated = false;
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = generateRandomPassword();
            generated = true;
        }

        User admin = User.builder()
                .fullName("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .isActive(true)
                .mustChangePassword(true)
                .build();

        userRepository.save(admin);

        if (generated) {
            System.out.println("=================================================");
            System.out.println("Admin account created: " + adminEmail);
            System.out.println("Temporary password: " + adminPassword);
            System.out.println("This password will NOT be shown again. Change it immediately after login.");
            System.out.println("=================================================");
        } else {
            System.out.println("Admin account created: " + adminEmail);
        }
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}