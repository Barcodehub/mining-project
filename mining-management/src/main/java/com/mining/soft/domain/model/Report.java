package com.mining.soft.domain.model;

import com.mining.soft.domain.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Report extends Post {
    private ReportStatus status;
    private boolean validationRequested;
    private Long validatedBy;
    private LocalDateTime validatedAt;

    @Override
    public void validate() {
        super.validate();
        if (this.status == null) {
            this.status = ReportStatus.SIN_VALIDAR;
        }
    }

    public void requestValidation() {
        if (this.status == ReportStatus.VALIDADO || this.status == ReportStatus.RECHAZADO) {
            throw new IllegalArgumentException("No se puede solicitar validación para un reporte que ya está " + this.status.name());
        }
        this.status = ReportStatus.PENDIENTE;
        this.validationRequested = true;
    }

    public void updateStatus(ReportStatus newStatus, Long validatorId) {
        if (this.status != ReportStatus.PENDIENTE) {
            throw new IllegalArgumentException("Solo se puede validar/rechazar un reporte en estado PENDIENTE");
        }
        if (newStatus == ReportStatus.VALIDADO || newStatus == ReportStatus.RECHAZADO) {
            this.status = newStatus;
            this.validatedBy = validatorId;
            this.validatedAt = LocalDateTime.now();
        } else {
            throw new IllegalArgumentException("Estado no válido para la actualización");
        }
    }
}
