package com.mining.soft.application.usecase;
import com.mining.soft.application.port.in.GetAuditLogsUseCase;
import com.mining.soft.application.port.out.AuditRepositoryPort;
import com.mining.soft.domain.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class GetAuditLogsUseCaseImpl implements GetAuditLogsUseCase {
    private final AuditRepositoryPort auditRepositoryPort;
    @Override
    public List<AuditLog> getAuditLogs(Long userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepositoryPort.findByFilters(userId, action, startDate, endDate);
    }
}
