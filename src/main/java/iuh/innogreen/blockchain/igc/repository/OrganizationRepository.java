package iuh.innogreen.blockchain.igc.repository;

import iuh.innogreen.blockchain.igc.entity.Organization;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Admin 2/15/2026
 *
 **/
@Repository
public interface OrganizationRepository extends JpaRepository<@NonNull Organization, @NonNull Long> {

    boolean existsByCode(String code);

    boolean existsByDomain(String domain);

    boolean existsByTaxCode(String taxCode);

}
