package com.mining.soft.infrastructure.input.dto;
import com.mining.soft.domain.enums.PostType;
import com.mining.soft.domain.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class ReportResponseDto extends PostResponseDto {
    private ReportStatus status;
    private boolean validationRequested;
    private Long validatedBy;
    private LocalDateTime validatedAt;
}
