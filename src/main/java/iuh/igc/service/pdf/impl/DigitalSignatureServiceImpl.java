package iuh.igc.service.pdf.impl;


import iuh.igc.advice.exception.InvalidCertificateException;
import iuh.igc.service.pdf.DigitalSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DigitalSignatureServiceImpl implements DigitalSignatureService {
    @Value("${signature.reason}")
    private String signatureReason;

    @Value("${signature.location}")
    private String signatureLocation;

    @Override
    public byte[] signPdfWithUserCertificate(byte[] pdfBytes, byte[] pkcs12Bytes, String password) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(pkcs12Bytes), password.toCharArray());

            String alias = findFirstPrivateKeyAlias(keyStore);
            if (alias == null) {
                throw new InvalidCertificateException("No private key found in uploaded certificate");
            }

            PrivateKey userPrivateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            Certificate[] userChain = keyStore.getCertificateChain(alias);

            if (userPrivateKey == null || userChain == null || userChain.length == 0) {
                throw new InvalidCertificateException("Invalid user certificate or password");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            PdfSigner signer = new PdfSigner(reader, baos, new StampingProperties().useAppendMode());
            signer.setFieldName("Signature-" + UUID.randomUUID());

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason(signatureReason);
            appearance.setLocation(signatureLocation);
            appearance.setReuseAppearance(false);
            appearance.setPageNumber(1);
            appearance.setPageRect(new Rectangle(0, 0, 0, 0));

            IExternalSignature pks = new PrivateKeySignature(
                    userPrivateKey,
                    DigestAlgorithms.SHA256,
                    BouncyCastleProvider.PROVIDER_NAME
            );

            IExternalDigest digest = new BouncyCastleDigest();

            signer.signDetached(
                    digest,
                    pks,
                    userChain,
                    null,
                    null,
                    null,
                    0,
                    PdfSigner.CryptoStandard.CMS
            );

            return baos.toByteArray();

        } catch (UnrecoverableKeyException e) {
            throw new InvalidCertificateException("Invalid certificate password");
        } catch (IOException e) {
            throw new InvalidCertificateException("Unable to read user certificate. Check file and password");
        } catch (InvalidCertificateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sign PDF with user certificate", e);
            throw new RuntimeException("Failed to sign PDF with user certificate", e);
        }
    }

    private String findFirstPrivateKeyAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Verify PDF signature
     */
    @Override
    public boolean verifyPdfSignature(byte[] signedPdfBytes) {
        try {
            log.info("🔍 Verifying PDF signature...");

            try (PdfReader reader = new PdfReader(new ByteArrayInputStream(signedPdfBytes));
                 com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader)) {

                SignatureUtil signUtil = new SignatureUtil(pdfDoc);
                List<String> names = signUtil.getSignatureNames();

                if (names.isEmpty()) {
                    log.warn("⚠️ No signatures found in PDF");
                    return false;
                }

                for (String name : names) {
                    try {
                        boolean coversWholeDoc = signUtil.signatureCoversWholeDocument(name);
                        int revision = signUtil.getRevision(name);
                        int totalRevisions = signUtil.getTotalRevisions();

                        PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);
                        boolean integrityValid = pkcs7.verifySignatureIntegrityAndAuthenticity();

                        String subject = "unknown";
                        String sigAlgorithm = "unknown";
                        if (pkcs7.getSigningCertificate() instanceof X509Certificate cert) {
                            subject = cert.getSubjectX500Principal().getName();
                            sigAlgorithm = cert.getSigAlgName();
                        }

                        log.info(
                                "Signature '{}': {} | coversWholeDocument={} | revision={}/{} | signDate={} | certSubject={} | certSigAlg={}",
                                name,
                                integrityValid ? "✅ Valid" : "❌ Invalid",
                                coversWholeDoc,
                                revision,
                                totalRevisions,
                                pkcs7.getSignDate() == null ? "n/a" : pkcs7.getSignDate().getTime(),
                                subject,
                                sigAlgorithm
                        );

                        if (!integrityValid) {
                            return false;
                        }
                    } catch (Exception signatureEx) {
                        log.error("Signature '{}' verification threw exception", name, signatureEx);
                        return false;
                    }
                }

                log.info("✅ PDF signature verified successfully");
                return true;
            }

        } catch (Exception e) {
            log.error("❌ Signature verification failed", e);
            return false;
        }
    }
}
