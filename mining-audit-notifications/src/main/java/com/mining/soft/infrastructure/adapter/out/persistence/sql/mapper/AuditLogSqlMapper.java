package com.mining.soft.infrastructure.adapter.out.persistence.sql.mapper;
import com.mining.soft.domain.model.AuditLog;
import com.mining.soft.infrastructure.adapter.out.persistence.sql.entity.AuditLogEntity;
import org.springframework.stereotype.Component;
@Component
public class AuditLogSqlMapper {
    public AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) return null;
        return AuditLogEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .action(domain.getAction())
                .entity(domain.getEntity())
                .entityId(domain.getEntityId())
                .timestamp(domain.getTimestamp())
                .metadata(domain.getMetadata())
                .build();
    }
    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) return null;
        return AuditLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .entity(entity.getEntity())
                .entityId(entity.getEntityId())
                .timestamp(entity.getTimestamp())
                .metadata(entity.getMetadata())
                .build();
    }
}
