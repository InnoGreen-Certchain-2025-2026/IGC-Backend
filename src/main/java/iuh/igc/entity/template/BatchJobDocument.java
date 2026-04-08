package iuh.igc.entity.template;

import iuh.igc.entity.constant.BatchStatus;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "batch_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchJobDocument {
    @Id
    String id;

    Long orgId;
    String templateId;
    String signatureImageId;
    String digitalCertId;

    BatchStatus status;

    Integer total;
    Integer completed;
    Integer failed;

    List<BatchError> errors;
    String zipStorageKey;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;
}
