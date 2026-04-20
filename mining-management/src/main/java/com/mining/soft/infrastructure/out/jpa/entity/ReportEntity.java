package com.mining.soft.infrastructure.out.jpa.entity;

import com.mining.soft.domain.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter @Setter
public class ReportEntity extends PostEntity {

    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.SIN_VALIDAR;

    private boolean validationRequested = false;

    private Long validatedBy;
    private LocalDateTime validatedAt;
}