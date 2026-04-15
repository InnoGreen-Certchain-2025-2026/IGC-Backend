package iuh.igc.dto.request;

import lombok.Data;

@Data
public class OTPVerifyRequest {
    private String email;
    private String otp;
}