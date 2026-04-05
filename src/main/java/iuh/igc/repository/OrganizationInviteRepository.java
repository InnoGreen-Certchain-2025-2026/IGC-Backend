package iuh.igc.repository;

import iuh.igc.entity.constant.OrganizationInviteStatus;
import iuh.igc.entity.organization.OrganizationInvite;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<@NonNull OrganizationInvite, @NonNull Long> {

    boolean existsByOrganization_IdAndInviteeEmailIgnoreCaseAndStatusAndExpiresAtAfter(
            Long organizationId,
            String inviteeEmail,
            OrganizationInviteStatus status,
            LocalDateTime now
    );

    Page<OrganizationInvite> findByInviteeEmailIgnoreCaseAndStatusAndExpiresAtAfter(
            String inviteeEmail,
            OrganizationInviteStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    Page<OrganizationInvite> findByOrganization_IdAndStatusAndExpiresAtAfter(
            Long organizationId,
            OrganizationInviteStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    long deleteByInviteeEmailIgnoreCaseAndExpiresAtBeforeAndStatus(
            String inviteeEmail,
            LocalDateTime now,
            OrganizationInviteStatus status
    );

    long deleteByInviteeEmailIgnoreCaseAndStatus(
            String inviteeEmail,
            OrganizationInviteStatus status
    );

    long deleteByOrganization_IdAndExpiresAtBeforeAndStatus(
            Long organizationId,
            LocalDateTime now,
            OrganizationInviteStatus status
    );

    long deleteByOrganization_IdAndStatus(
            Long organizationId,
            OrganizationInviteStatus status
    );

    long deleteByOrganization_IdAndInviteeEmailIgnoreCaseAndExpiresAtBeforeAndStatus(
            Long organizationId,
            String inviteeEmail,
            LocalDateTime now,
            OrganizationInviteStatus status
    );

    long deleteByOrganization_IdAndInviteeEmailIgnoreCaseAndStatus(
            Long organizationId,
            String inviteeEmail,
            OrganizationInviteStatus status
    );

    Optional<OrganizationInvite> findByInviteToken(String inviteToken);
}
