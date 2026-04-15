package iuh.igc.service.otp;

public interface OtpService {
    void sendOtp(String email);
    boolean verifyOtp(String email, String otp);
}
