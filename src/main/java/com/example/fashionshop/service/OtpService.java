package com.example.fashionshop.service;

import com.example.fashionshop.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final Map<String, String> otpStore = new HashMap<>();
    private final Map<String, Long> otpExpiry = new HashMap<>();

    private final EmailService emailService;

    public void generateAndSend(String email) {
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        otpStore.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + 5 * 60 * 1000); // 5 phút
        emailService.sendOtpEmail(email, otp);
    }

    public boolean verifyOtp(String email, String otp) {
        if (!otpStore.containsKey(email)) return false;
        if (System.currentTimeMillis() > otpExpiry.get(email)) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            return false;
        }
        return otpStore.get(email).equals(otp);
    }

    public void clearOtp(String email) {
        otpStore.remove(email);
        otpExpiry.remove(email);
    }
}
