package iuh.igc.repository;

import iuh.igc.entity.organization.Organization;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Admin 2/15/2026
 *
 **/
@Repository
public interface OrganizationRepository extends JpaRepository<@NonNull Organization, @NonNull Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByDomain(String domain);

    boolean existsByDomainAndIdNot(String domain, Long id);

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndIdNot(String taxCode, Long id);

    Page<@NonNull Organization> findDistinctByOrganizationMembers_User_Id(Long userId, Pageable pageable);

    Page<@NonNull Organization> findDistinctByOrganizationMembers_User_IdOrderByNameDesc(
            Long userId,
            Pageable pageable
    );

    Optional<Organization> findByIdAndOrganizationMembers_User_Id(Long id, Long userId);

    Optional<Organization> findByCode(String code);

}
