package iuh.igc.repository;

import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.OrganizationMember;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Admin 2/15/2026
 *
 **/
@Repository
public interface OrganizationMemberRepository extends JpaRepository<@NonNull OrganizationMember, @NonNull Long> {
    boolean existsByOrganization_IdAndUser_Id(Long organizationId, Long userId);

    Optional<OrganizationMember> findByOrganization_IdAndUser_Id(Long organizationId, Long userId);

    Page<OrganizationMember> findByOrganization_Id(Long organizationId, Pageable pageable);

    boolean existsByOrganization_IdAndUser_IdAndOrganizationRole(
            Long organizationId,
            Long userId,
            OrganizationRole organizationRole
    );

    boolean existsByOrganization_IdAndUser_IdAndOrganizationRoleIn(
            Long organizationId,
            Long userId,
            Collection<OrganizationRole> organizationRoles
    );

        List<OrganizationMember> findByUser_IdAndOrganizationRoleIn(Long userId, Collection<OrganizationRole> roles);
}
