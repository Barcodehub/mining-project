package com.mining.soft.infrastructure.input.dto;

import com.mining.soft.domain.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportStatusRequestDto {
    private ReportStatus status;
    private Long validatorId;
}
