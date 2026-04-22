package com.mining.soft.infrastructure.adapter.out.persistence.sql.repository;
import com.mining.soft.infrastructure.adapter.out.persistence.sql.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface SpringDataAuditRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {
}
