package iuh.igc.entity;

import iuh.igc.entity.base.BaseEntity;
import iuh.igc.entity.constant.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Admin 2/9/2026
 *
 **/
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {

    /**
     * =============================================
     * THÔNG TIN ĐỊNH DANH
     * =============================================
     **/

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true)
    String email;

    @Column(nullable = false)
    String hashedPassword;

    @Column(name = "cognito_sub", unique = true)
    String cognitoSub;

    /**
     * =============================================
     * THÔNG TIN CHUNG
     * =============================================
     **/

    // Keep the column nullable at the database level so schema auto-update
    // can add it to an existing populated table without failing startup.
    // Request validation still requires this field for newly created users.
    @Column(unique = true)
    String citizenIdNumber;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String phoneNumber;

    @Column(nullable = false)
    String address;

    @Column(nullable = false)
    LocalDate dob;

    @Column(nullable = false)
    Gender gender;

    @Column(nullable = true, name = "avatar_url")
    String avatarUrl;
}
