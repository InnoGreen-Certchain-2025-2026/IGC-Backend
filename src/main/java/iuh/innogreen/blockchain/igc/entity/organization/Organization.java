package iuh.innogreen.blockchain.igc.entity.organization;

import iuh.innogreen.blockchain.igc.entity.base.BaseEntity;
import iuh.innogreen.blockchain.igc.entity.constant.ServicePlan;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Admin 2/15/2026
 *
 **/
@Entity
@Table(
        name = "organizations",
        indexes = {
                @Index(name = "idx_org_code", columnList = "code"),
                @Index(name = "idx_org_tax_code", columnList = "tax_code"),
                @Index(name = "idx_org_domain", columnList = "domain")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * =============================================
     * THÔNG TIN CHUNG
     * =============================================
     **/

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String code;

    @Column(nullable = true, unique = true)
    String domain;

    @Column(nullable = true)
    String logoUrl;

    @Column(nullable = true)
    String description;

    /**
     * =============================================
     * THÔNG TIN PHÁP LÝ
     * =============================================
     **/

    @Column(nullable = false)
    String legalName; // Tên pháp lý

    @Column(nullable = false, unique = true)
    String taxCode; // Mã số thuế

    @Column(nullable = false)
    String legalAddress; // Địa chỉ trụ sở

    @Column(nullable = true)
    String representativeName; // Người đại diện theo pháp luật

    /**
     * =============================================
     * THÔNG TIN LIÊN HỆ
     * =============================================
     **/

    @Column(nullable = false)
    String contactName;

    @Column(nullable = false)
    String contactEmail;

    @Column(nullable = false)
    String contactPhone;

    /**
     * =============================================
     * Gói dịch vụ
     * =============================================
     **/

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ServicePlan servicePlan;

    /**
     * =============================================
     * Mối quan hệ ràng buộc
     * =============================================
     **/

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    List<OrganizationMember> organizationMembers;


}
