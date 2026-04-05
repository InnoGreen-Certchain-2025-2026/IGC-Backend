package iuh.igc.entity.organization;

import iuh.igc.entity.User;
import iuh.igc.entity.base.BaseEntity;
import iuh.igc.entity.constant.OrganizationRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Admin 2/15/2026
 *
 **/
@Entity
@Table(
        name = "organization_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_member", columnNames = {"organization_id", "user_id"})
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class OrganizationMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    OrganizationRole organizationRole;

}
