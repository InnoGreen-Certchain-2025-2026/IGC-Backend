package iuh.innogreen.blockchain.igc.repository;

import iuh.innogreen.blockchain.igc.entity.OrganizationMember;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Admin 2/15/2026
 *
 **/
@Repository
public interface OrganizationMemberRepository extends JpaRepository<@NonNull OrganizationMember, @NonNull Long> {
}
