package org.example.gezhiplatform.repository;

import org.example.gezhiplatform.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long>, JpaSpecificationExecutor<Audit> {
    void deleteByUser_IdIn(Collection<Long> userIds);
}
