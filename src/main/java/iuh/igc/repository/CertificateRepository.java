package iuh.igc.repository;

import iuh.igc.entity.Certificate;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<@NonNull Certificate, @NonNull Long> {

    Optional<Certificate> findByCertificateId(String certificateId);

    Optional<Certificate> findBySignedPdfHash(String signedPdfHash);

    Optional<Certificate> findByClaimCode(String claimCode);

    List<Certificate> findCertificateByStudentId(Long studentId);

    List<Certificate> findCertificateByIssuer(String issuer);

    boolean existsByClaimCode(String claimCode);

    boolean existsByCertificateId(String certificateId);
}
