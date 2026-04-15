package iuh.igc.service.email;

import iuh.igc.dto.request.contact.ContactFormRequest;

public interface EmailService {
    void sendOTPEmail(String toEmail, String otp);

    void sendContactFormEmail(ContactFormRequest request);
}