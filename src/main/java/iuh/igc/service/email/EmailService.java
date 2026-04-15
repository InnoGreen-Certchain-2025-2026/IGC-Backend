package iuh.igc.service.email;

public interface EmailService {
    void sendOTPEmail(String toEmail, String otp);
}