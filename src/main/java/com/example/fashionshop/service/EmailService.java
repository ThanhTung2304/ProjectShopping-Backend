package com.example.fashionshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@leanh-studio.com");
        message.setTo(toEmail);
        message.setSubject("Mã OTP đặt lại mật khẩu - LeAnh Studio");
        message.setText(
                "Xin chào,\n\n" +
                        "Mã OTP của bạn là: " + otp + "\n\n" +
                        "Mã có hiệu lực trong 5 phút.\n" +
                        "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n" +
                        "LeAnh Studio"
        );
        mailSender.send(message);
    }
}