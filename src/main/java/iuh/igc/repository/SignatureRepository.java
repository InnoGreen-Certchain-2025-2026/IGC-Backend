package iuh.igc.repository;

import iuh.igc.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    Signature findByOrganizationIdAndIsActiveTrue(Long organizationId);
    Boolean existsByOrganizationIdAndHash(Long organizationId, String hash);
    Boolean removeByOrganizationIdAndHash(Long organizationId, String hash);
}
