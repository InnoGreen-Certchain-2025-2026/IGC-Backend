package iuh.igc.service.organization.impl;

import iuh.igc.dto.request.organization.CreateOrganizationInviteRequest;
import iuh.igc.dto.response.orginazation.OrganizationInviteResponse;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.OrganizationInviteStatus;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.Organization;
import iuh.igc.entity.organization.OrganizationInvite;
import iuh.igc.entity.organization.OrganizationMember;
import iuh.igc.repository.OrganizationInviteRepository;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.repository.UserRepository;
import iuh.igc.service.organization.OrganizationInviteService;
import iuh.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationInviteServiceImpl implements OrganizationInviteService {

    OrganizationInviteRepository organizationInviteRepository;
    OrganizationRepository organizationRepository;
    OrganizationMemberRepository organizationMemberRepository;
    UserRepository userRepository;
    CurrentUserProvider currentUserProvider;

    static int INVITE_EXPIRE_DAYS = 7;

    /**
     * =============================================
     * List invites
     * =============================================
     **/

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationInviteResponse> getInvitesByUserId(Long userId, Pageable pageable) {
        User currentUser = currentUserProvider.get();
        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách lời mời này");
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        cleanupExpiredInvitesByEmail(user.getEmail());

        return organizationInviteRepository
                .findByInviteeEmailIgnoreCaseAndStatusAndExpiresAtAfter(
                        user.getEmail(),
                        OrganizationInviteStatus.PENDING,
                        LocalDateTime.now(),
                        pageable
                )
                .map(this::mapToOrganizationInviteResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationInviteResponse> getInvitesByOrganization(Long organizationId, Pageable pageable) {
        User currentUser = currentUserProvider.get();
        validateCanInvite(organizationId, currentUser.getId());

        cleanupExpiredInvitesByOrganization(organizationId);

        return organizationInviteRepository
                .findByOrganization_IdAndStatusAndExpiresAtAfter(
                        organizationId,
                        OrganizationInviteStatus.PENDING,
                        LocalDateTime.now(),
                        pageable
                )
                .map(this::mapToOrganizationInviteResponse);
    }

    /**
     * =============================================
     * Invite user (chỉ OWNER/MODERATOR)
     * =============================================
     **/

    @Override
    @Transactional
    public String inviteUser(Long organizationId, CreateOrganizationInviteRequest request) {
        User inviter = currentUserProvider.get();
        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tổ chức"));

        validateCanInvite(organizationId, inviter.getId());

        String inviteeEmail = request.inviteeEmail().trim().toLowerCase();
        OrganizationRole invitedRole = request.invitedRole();

        if (invitedRole == OrganizationRole.OWNER)
            throw new DataIntegrityViolationException("Không thể mời vai trò OWNER");

        Optional<User> inviteeUser = userRepository.findByEmail(inviteeEmail);
        if (inviteeUser.isPresent() && organizationMemberRepository.existsByOrganization_IdAndUser_Id(
                organizationId,
                inviteeUser.get().getId()
        )) {
            throw new DataIntegrityViolationException("Người dùng đã là thành viên của tổ chức");
        }

        cleanupExpiredInvites(organizationId, inviteeEmail);

        boolean hasPendingInvite = organizationInviteRepository
                .existsByOrganization_IdAndInviteeEmailIgnoreCaseAndStatusAndExpiresAtAfter(
                        organizationId,
                        inviteeEmail,
                        OrganizationInviteStatus.PENDING,
                        LocalDateTime.now()
                );

        if (hasPendingInvite)
            throw new DataIntegrityViolationException("Đã có lời mời đang chờ cho email này");

        OrganizationInvite invite = OrganizationInvite
                .builder()
                .organization(organization)
                .inviterUser(inviter)
                .inviteeUser(inviteeUser.orElse(null))
                .inviteeEmail(inviteeEmail)
                .invitedRole(invitedRole)
                .status(OrganizationInviteStatus.PENDING)
                .inviteMessage(normalizeNullable(request.inviteMessage()))
                .inviteToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRE_DAYS))
                .build();

        return organizationInviteRepository.save(invite).getInviteToken();
    }

    /**
     * =============================================
     * Invite response
     * =============================================
     **/

    @Override
    @Transactional
    public void acceptInvite(String inviteToken) {
        User currentUser = currentUserProvider.get();
        OrganizationInvite invite = findValidPendingInvite(inviteToken, currentUser);

        if (organizationMemberRepository.existsByOrganization_IdAndUser_Id(
                invite.getOrganization().getId(),
                currentUser.getId()
        )) {
            throw new DataIntegrityViolationException("Người dùng đã là thành viên của tổ chức");
        }

        OrganizationMember member = OrganizationMember
                .builder()
                .organization(invite.getOrganization())
                .user(currentUser)
                .organizationRole(invite.getInvitedRole())
                .build();
        organizationMemberRepository.save(member);
        organizationInviteRepository.delete(invite);
    }

    @Override
    @Transactional
    public void declineInvite(String inviteToken) {
        User currentUser = currentUserProvider.get();
        OrganizationInvite invite = findValidPendingInvite(inviteToken, currentUser);
        organizationInviteRepository.delete(invite);
    }

    @Override
    @Transactional
    public void cancelInvite(String inviteToken) {
        User currentUser = currentUserProvider.get();
        OrganizationInvite invite = organizationInviteRepository
                .findByInviteToken(inviteToken)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lời mời"));

        validateCanInvite(invite.getOrganization().getId(), currentUser.getId());

        if (invite.getStatus() != OrganizationInviteStatus.PENDING)
            throw new DataIntegrityViolationException("Chỉ có thể hủy lời mời ở trạng thái chờ");

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            organizationInviteRepository.delete(invite);
            throw new DataIntegrityViolationException("Lời mời đã hết hạn");
        }

        organizationInviteRepository.delete(invite);
    }

    /**
     * =============================================
     * Helper
     * =============================================
     **/

    private void validateCanInvite(Long organizationId, Long userId) {
        boolean canInvite = organizationMemberRepository.existsByOrganization_IdAndUser_IdAndOrganizationRoleIn(
                organizationId,
                userId,
                List.of(OrganizationRole.OWNER, OrganizationRole.MODERATOR)
        );

        if (!canInvite)
            throw new AccessDeniedException("Chỉ OWNER hoặc MODERATOR mới có quyền mời thành viên");
    }

    private OrganizationInvite findValidPendingInvite(String inviteToken, User currentUser) {
        OrganizationInvite invite = organizationInviteRepository
                .findByInviteToken(inviteToken)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lời mời"));

        if (!invite.getInviteeEmail().equalsIgnoreCase(currentUser.getEmail()))
            throw new AccessDeniedException("Bạn không có quyền phản hồi lời mời này");

        if (invite.getStatus() != OrganizationInviteStatus.PENDING)
            throw new DataIntegrityViolationException("Lời mời không còn ở trạng thái chờ");

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            organizationInviteRepository.delete(invite);
            throw new DataIntegrityViolationException("Lời mời đã hết hạn");
        }

        return invite;
    }

    private void cleanupExpiredInvites(Long organizationId, String inviteeEmail) {
        LocalDateTime now = LocalDateTime.now();
        organizationInviteRepository.deleteByOrganization_IdAndInviteeEmailIgnoreCaseAndExpiresAtBeforeAndStatus(
                organizationId,
                inviteeEmail,
                now,
                OrganizationInviteStatus.PENDING
        );
        organizationInviteRepository.deleteByOrganization_IdAndInviteeEmailIgnoreCaseAndStatus(
                organizationId,
                inviteeEmail,
                OrganizationInviteStatus.EXPIRED
        );
    }

    private void cleanupExpiredInvitesByEmail(String inviteeEmail) {
        LocalDateTime now = LocalDateTime.now();
        organizationInviteRepository.deleteByInviteeEmailIgnoreCaseAndExpiresAtBeforeAndStatus(
                inviteeEmail,
                now,
                OrganizationInviteStatus.PENDING
        );
        organizationInviteRepository.deleteByInviteeEmailIgnoreCaseAndStatus(
                inviteeEmail,
                OrganizationInviteStatus.EXPIRED
        );
    }

    private void cleanupExpiredInvitesByOrganization(Long organizationId) {
        LocalDateTime now = LocalDateTime.now();
        organizationInviteRepository.deleteByOrganization_IdAndExpiresAtBeforeAndStatus(
                organizationId,
                now,
                OrganizationInviteStatus.PENDING
        );
        organizationInviteRepository.deleteByOrganization_IdAndStatus(
                organizationId,
                OrganizationInviteStatus.EXPIRED
        );
    }

    private OrganizationInviteResponse mapToOrganizationInviteResponse(OrganizationInvite invite) {
        Organization organization = invite.getOrganization();
        User inviter = invite.getInviterUser();
        return OrganizationInviteResponse
                .builder()
                .id(invite.getId())
                .inviteToken(invite.getInviteToken())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .organizationCode(organization.getCode())
                .organizationLogoUrl(organization.getLogoUrl())
                .inviteeEmail(invite.getInviteeEmail())
                .inviterName(inviter.getName())
                .inviterEmail(inviter.getEmail())
                .invitedRole(invite.getInvitedRole())
                .status(invite.getStatus())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .build();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
