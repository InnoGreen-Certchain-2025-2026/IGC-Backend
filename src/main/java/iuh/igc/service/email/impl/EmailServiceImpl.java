package iuh.igc.service.email.impl;

import iuh.igc.dto.request.contact.ContactFormRequest;
import iuh.igc.service.email.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Autowired
    private TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    @Value("${contact.recipient-email:igcertchain@gmail.com}")
    private String contactRecipientEmail;

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

    @Override
    public void sendContactFormEmail(ContactFormRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(contactRecipientEmail);
            helper.setReplyTo(request.email());
            helper.setSubject("[IGC Contact] Yêu cầu liên hệ mới");
            helper.setText(buildHtmlContact(request), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Send contact email failed", e);
        }
    }
    private String buildHtmlContact(ContactFormRequest request) {
        Context context = new Context();
        context.setVariable("fullName", request.fullName());
        context.setVariable("email", request.email());
        context.setVariable("company", request.company());
        context.setVariable("description", request.description());

        return templateEngine.process("contact-email", context);
    }
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
                """
                .formatted(otp);
    }
}