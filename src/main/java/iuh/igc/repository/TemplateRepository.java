package iuh.igc.repository;

import iuh.igc.entity.template.TemplateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends MongoRepository<TemplateDocument, String> {
    List<TemplateDocument> findByOrgId(Long orgId);
    List<TemplateDocument> findByOrgIdAndNameContainingIgnoreCase(Long orgId, String keyword);
    Optional<TemplateDocument> findByIdAndOrgId(String id, Long orgId);
}