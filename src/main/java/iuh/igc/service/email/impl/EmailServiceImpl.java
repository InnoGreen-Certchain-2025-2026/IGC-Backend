package iuh.igc.service.email.impl;

import iuh.igc.service.email.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOTPEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String html = buildHtmlOtp(otp);

            helper.setTo(toEmail);
            helper.setSubject("Xác thực OTP");
            helper.setText(html, true); // true = HTML

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Send email failed", e);
        }
    }

    private String buildHtmlOtp(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial; background:#f4f6f8; padding:20px;">
                <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px; text-align:center;">
                    <h2>Xác thực tài khoản</h2>
                    <p>Mã OTP của bạn là:</p>
                    <h1 style="color:#2d89ef; letter-spacing:5px;">%s</h1>
                    <p>Mã có hiệu lực trong 5 phút.</p>
                </div>
            </body>
            </html>
            """.formatted(otp);
    }
}