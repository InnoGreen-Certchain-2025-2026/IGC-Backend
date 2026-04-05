package iuh.igc.service.organization.impl;

import iuh.igc.dto.response.orginazation.OrganizationMemberResponse;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.OrganizationMember;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.service.organization.OrganizationMemberService;
import iuh.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    OrganizationMemberRepository organizationMemberRepository;
    CurrentUserProvider currentUserProvider;

    /**
     * =============================================
     * List members
     * =============================================
     **/

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationMemberResponse> getOrganizationMembers(Long organizationId, Pageable pageable) {
        User actor = currentUserProvider.get();

        boolean isMember = organizationMemberRepository
                .existsByOrganization_IdAndUser_Id(organizationId, actor.getId());
        if (!isMember) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách thành viên");
        }

        return organizationMemberRepository
                .findByOrganization_Id(organizationId, pageable)
                .map(this::mapToOrganizationMemberResponse);
    }

    /**
     * =============================================
     * Role management (chỉ OWNER)
     * =============================================
     **/

    @Override
    @Transactional
    public void promoteToModerator(Long organizationId, Long targetUserId) {
        User actor = currentUserProvider.get();
        OrganizationMember actorMember = getMemberOrThrow(organizationId, actor.getId());
        OrganizationMember targetMember = getMemberOrThrow(organizationId, targetUserId);

        ensureOwner(actorMember);

        if (targetMember.getOrganizationRole() == OrganizationRole.OWNER)
            throw new DataIntegrityViolationException("Không thể thay đổi vai trò OWNER");

        if (targetMember.getOrganizationRole() == OrganizationRole.MODERATOR)
            return;

        targetMember.setOrganizationRole(OrganizationRole.MODERATOR);
    }

    @Override
    @Transactional
    public void demoteToMember(Long organizationId, Long targetUserId) {
        User actor = currentUserProvider.get();
        OrganizationMember actorMember = getMemberOrThrow(organizationId, actor.getId());
        OrganizationMember targetMember = getMemberOrThrow(organizationId, targetUserId);

        ensureOwner(actorMember);

        if (targetMember.getOrganizationRole() == OrganizationRole.OWNER)
            throw new DataIntegrityViolationException("Không thể thay đổi vai trò OWNER");

        if (targetMember.getOrganizationRole() == OrganizationRole.MEMBER)
            return;

        targetMember.setOrganizationRole(OrganizationRole.MEMBER);
    }

    /**
     * =============================================
     * Kick member
     * =============================================
     **/

    @Override
    @Transactional
    public void kickMember(Long organizationId, Long targetUserId) {
        User actor = currentUserProvider.get();
        OrganizationMember actorMember = getMemberOrThrow(organizationId, actor.getId());
        OrganizationMember targetMember = getMemberOrThrow(organizationId, targetUserId);

        if (targetMember.getOrganizationRole() == OrganizationRole.OWNER)
            throw new DataIntegrityViolationException("Không thể kick OWNER");

        if (targetMember.getOrganizationRole() == OrganizationRole.MODERATOR) {
            ensureOwner(actorMember);
        } else if (targetMember.getOrganizationRole() == OrganizationRole.MEMBER) {
            ensureOwnerOrModerator(actorMember);
        }

        organizationMemberRepository.delete(targetMember);
    }

    /**
     * =============================================
     * Helper
     * =============================================
     **/

    private OrganizationMember getMemberOrThrow(Long organizationId, Long userId) {
        return organizationMemberRepository
                .findByOrganization_IdAndUser_Id(organizationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên trong tổ chức"));
    }

    private OrganizationMemberResponse mapToOrganizationMemberResponse(OrganizationMember member) {
        User user = member.getUser();
        return OrganizationMemberResponse
                .builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getOrganizationRole())
                .build();
    }

    private void ensureOwner(OrganizationMember member) {
        if (member.getOrganizationRole() != OrganizationRole.OWNER)
            throw new AccessDeniedException("Chỉ OWNER mới có quyền thực hiện thao tác này");
    }

    private void ensureOwnerOrModerator(OrganizationMember member) {
        if (member.getOrganizationRole() != OrganizationRole.OWNER
                && member.getOrganizationRole() != OrganizationRole.MODERATOR) {
            throw new AccessDeniedException("Chỉ OWNER hoặc MODERATOR mới có quyền thực hiện thao tác này");
        }
    }
}
