package iuh.igc.controller;

import iuh.igc.dto.request.OTPRequest;
import iuh.igc.dto.request.OTPVerifyRequest;
import iuh.igc.service.otp.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OTPController {

    private final OtpService otpService;

    // ✅ Gửi OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody OTPRequest request) {
        otpService.sendOtp(request.getEmail());
        return ResponseEntity.ok("OTP sent");
    }

    // ✅ Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody OTPVerifyRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(isValid);
    }
}