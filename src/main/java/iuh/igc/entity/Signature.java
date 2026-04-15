package iuh.igc.entity;
import iuh.igc.entity.organization.Organization;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "signatures",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "hash"})}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String hash;

    private boolean isActive;

    private LocalDateTime createdAt;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "image_s3_url")
    private String imageS3Url;

    @Column(name = "image_s3_key")
    private String imageS3Key;

}