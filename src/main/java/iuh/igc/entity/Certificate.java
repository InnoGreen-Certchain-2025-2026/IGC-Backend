package iuh.igc.entity;

import iuh.igc.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certificate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "certificate_id", unique = true, nullable = false)
    String certificateId;

    @Column(name = "student_name", nullable = false)
    String studentName;

    @Column(name = "student_id", nullable = false)
    String studentId;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "major")
    String major;

    @Column(name = "graduation_year")
    Integer graduationYear;

    @Column(name = "gpa")
    Double gpa;

    @Column(name = "certificate_type")
    String certificateType;

    @Column(name = "issuer", nullable = false)
    String issuer;

    @Column(name = "issue_date")
    LocalDate issueDate;

    @Column(name = "pdf_filename")
    private String pdfFilename;

    @Column(name = "pdf_s3_path")
    private String pdfS3Path;

    @Column(name = "pdf_s3_url")
    private String pdfS3Url;

    @Column(name = "pdf_size_bytes")
    private Long pdfSizeBytes;

    // Hash pdf signed
    @Column(name = "signed_pdf_hash", nullable = false, unique = true, length = 66)
    private String signedPdfHash;

    // Thông tin chữ ký số
    @Column(name = "signature_timestamp")
    private LocalDateTime signatureTimestamp;

    @Column(name = "signer_name")
    private String signerName;

    @Column(name = "document_hash", nullable = false, unique = true, length = 66)
    String documentHash;

    @Column(name = "blockchain_tx_hash", length = 66)
    String blockchainTxHash;

    @Column(name = "blockchain_block_number")
    Long blockchainBlockNumber;

    @Column(name = "blockchain_timestamp")
    Long blockchainTimestamp;

    @Column(name = "is_valid")
    Boolean isValid = true;
}
