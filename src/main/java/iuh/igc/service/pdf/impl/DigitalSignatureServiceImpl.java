package iuh.igc.service.pdf.impl;


import com.itextpdf.kernel.pdf.PdfDocument;
import iuh.igc.service.pdf.DigitalSignatureService;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DigitalSignatureServiceImpl implements DigitalSignatureService {
    private final ResourceLoader resourceLoader;

    @Value("${signature.keystore-path}")
    private String keystorePath;

    @Value("${signature.keystore-password}")
    private String keystorePassword;

    @Value("${signature.key-alias}")
    private String keyAlias;

    @Value("${signature.reason}")
    private String signatureReason;

    @Value("${signature.location}")
    private String signatureLocation;

    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    public DigitalSignatureServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            log.info("🔐 Initializing Digital Signature...");

            // Add Bouncy Castle Provider
            Security.addProvider(new BouncyCastleProvider());

            // Load keystore
            Resource keystoreResource = resourceLoader.getResource(keystorePath);
            KeyStore keystore = KeyStore.getInstance("PKCS12");

            try (InputStream is = keystoreResource.getInputStream()) {
                keystore.load(is, keystorePassword.toCharArray());
            }

            // Get private key and certificate chain
            privateKey = (PrivateKey) keystore.getKey(
                    keyAlias,
                    keystorePassword.toCharArray()
            );
            certificateChain = keystore.getCertificateChain(keyAlias);

            if (privateKey == null || certificateChain == null) {
                throw new RuntimeException("Failed to load signing credentials");
            }

            log.info("✅ Digital signature initialized");
            log.info("   Certificate: {}",
                    ((java.security.cert.X509Certificate) certificateChain[0])
                            .getSubjectX500Principal().getName());

        } catch (Exception e) {
            log.error("❌ Failed to initialize digital signature", e);
            throw new RuntimeException("Digital signature initialization failed", e);
        }
    }

    /**
     * Sign PDF document
     */
    @Override
    public byte[] signPdf(byte[] pdfBytes, String certificateId) {
        try {
            log.info("✍️ Signing PDF for certificate: {}", certificateId);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            PdfSigner signer = new PdfSigner(
                    reader,
                    baos,
                    new StampingProperties()
            );

            // Create signature appearance
            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason(signatureReason);
            appearance.setLocation(signatureLocation);
            appearance.setReuseAppearance(false);

            // Create digital signature
            IExternalSignature pks = new PrivateKeySignature(
                    privateKey,
                    DigestAlgorithms.SHA256,
                    BouncyCastleProvider.PROVIDER_NAME
            );

            IExternalDigest digest = new BouncyCastleDigest();

            // Sign the document
            signer.signDetached(
                    digest,
                    pks,
                    certificateChain,
                    null,
                    null,
                    null,
                    0,
                    PdfSigner.CryptoStandard.CMS
            );

            byte[] signedPdf = baos.toByteArray();
            log.info("✅ PDF signed successfully - Size: {} bytes", signedPdf.length);

            return signedPdf;

        } catch (Exception e) {
            log.error("❌ Failed to sign PDF", e);
            throw new RuntimeException("PDF signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify PDF signature
     */
    @Override
    public boolean verifyPdfSignature(byte[] signedPdfBytes) {
        try {
            log.info("🔍 Verifying PDF signature...");

            PdfReader reader = new PdfReader(new ByteArrayInputStream(signedPdfBytes));
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader);

            SignatureUtil signUtil = new SignatureUtil(pdfDoc);
            java.util.List<String> names = signUtil.getSignatureNames();

            if (names.isEmpty()) {
                log.warn("⚠️ No signatures found in PDF");
                return false;
            }

            for (String name : names) {
                PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);
                boolean valid = pkcs7.verifySignatureIntegrityAndAuthenticity();

                log.info("Signature '{}': {}", name, valid ? "✅ Valid" : "❌ Invalid");

                if (!valid) {
                    return false;
                }
            }

            pdfDoc.close();
            log.info("✅ PDF signature verified successfully");
            return true;

        } catch (Exception e) {
            log.error("❌ Signature verification failed", e);
            return false;
        }
    }
}
