package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.request.contact.ContactFormRequest;
import iuh.igc.service.email.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contact")
public class ContactController {

    private final EmailService emailService;

    @PostMapping
    public ApiResponse<@NonNull Void> submitContact(
            @RequestBody @Valid ContactFormRequest request) {
        emailService.sendContactFormEmail(request);
        return ApiResponse.<Void>builder().build();
    }
}
