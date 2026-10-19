package com.mining.soft.infrastructure.adapter.out.persistence.sql.repository;
import com.mining.soft.application.port.out.AuditRepositoryPort;
import com.mining.soft.domain.model.AuditLog;
import com.mining.soft.infrastructure.adapter.out.persistence.sql.entity.AuditLogEntity;
import com.mining.soft.infrastructure.adapter.out.persistence.sql.mapper.AuditLogSqlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepositoryPort {
    private final SpringDataAuditRepository repository;
    private final AuditLogSqlMapper mapper;
    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = mapper.toEntity(auditLog);
        entity = repository.save(entity);
        return mapper.toDomain(entity);
    }
    @Override
    public boolean exists(String entity, Long entityId, String action) {
        return repository.existsByEntityAndEntityIdAndAction(entity, entityId, action);
    }
    @Override
    public List<AuditLog> findByFilters(Long userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
            if (action != null && !action.isEmpty()) predicates.add(cb.equal(root.get("action"), action));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
