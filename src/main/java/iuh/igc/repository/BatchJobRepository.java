package iuh.igc.repository;

import iuh.igc.entity.constant.BatchStatus;
import iuh.igc.entity.template.BatchJobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchJobRepository extends MongoRepository<BatchJobDocument, String> {
    List<BatchJobDocument> findByOrgIdOrderByCreatedAtDesc(String orgId);
    List<BatchJobDocument> findByOrgIdAndStatus(String orgId, BatchStatus status);
}