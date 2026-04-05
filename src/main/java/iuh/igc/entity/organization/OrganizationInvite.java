package iuh.igc.entity.organization;

import iuh.igc.entity.User;
import iuh.igc.entity.base.BaseEntity;
import iuh.igc.entity.constant.OrganizationInviteStatus;
import iuh.igc.entity.constant.OrganizationRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "organization_invites",
        indexes = {
                @Index(name = "idx_org_invite_org", columnList = "organization_id"),
                @Index(name = "idx_org_invite_email", columnList = "invitee_email"),
                @Index(name = "idx_org_invite_status", columnList = "status"),
                @Index(name = "idx_org_invite_token", columnList = "invite_token")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_org_invite_token", columnNames = "invite_token")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    User inviterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_user_id")
    User inviteeUser;

    @Column(name = "invitee_email", nullable = false, length = 254)
    String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "invited_role", nullable = false, length = 20)
    OrganizationRole invitedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    OrganizationInviteStatus status = OrganizationInviteStatus.PENDING;

    @Column(name = "invite_token", nullable = false, length = 100)
    String inviteToken;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @Column(name = "invite_message", length = 500)
    String inviteMessage;
}
