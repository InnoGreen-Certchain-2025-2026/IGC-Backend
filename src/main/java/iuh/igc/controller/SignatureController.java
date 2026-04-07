package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.entity.Signature;
import iuh.igc.entity.organization.Organization;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.repository.SignatureRepository;
import iuh.igc.service.opencv.SignatureService;
import iuh.igc.service.pdf.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signature")
public class SignatureController {

    @Autowired
    private SignatureService signatureService;
    @Autowired
    private HashService hashService;
    @Autowired
    private SignatureRepository signatureRepository;
    @Autowired
    private OrganizationRepository organizationRepository;


    @PostMapping("/upload")
    public ApiResponse<Boolean> uploadSignature(
            @RequestParam Long orgId,
            @RequestParam MultipartFile file
    ){
        signatureService.createSignature(orgId, file);
        return new ApiResponse<>(true);
    }

    @PostMapping("/check")
    public ApiResponse<Boolean> checkSignature(
            @RequestParam Long orgId,
            @RequestParam MultipartFile file
    ){
        if(signatureService.checkSignatureIsUsed(file, orgId)){
            return new ApiResponse<>(true);
        } else {
            return new ApiResponse<>(false);
        }
    }
}
