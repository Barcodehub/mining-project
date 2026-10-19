package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.infrastructure.input.dto.ReportResponseDto;
import com.mining.soft.infrastructure.input.dto.ReportStatusRequestDto;
import com.mining.soft.infrastructure.input.mapper.IPostDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final IPostServicePort postServicePort;
    private final IPostDtoMapper postDtoMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    // Cache only when no filters are provided to avoid creating many cache keys for different filter combinations
    @Cacheable(value = "reports", key = "'reports_all'", condition = "#status==null && #startDate==null && #endDate==null && #minLat==null && #maxLat==null && #minLng==null && #maxLng==null")
    public ResponseEntity<List<ReportResponseDto>> filterReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDtoList(
                postServicePort.filterReports(status, startDate, endDate, minLat, maxLat, minLng, maxLng)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponseDto> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(postServicePort.getReportById(id)));
    }

    @PatchMapping("/{id}/request-validation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponseDto> requestValidation(@PathVariable Long id) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(postServicePort.requestValidation(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponseDto> changeStatus(@PathVariable Long id, @RequestBody ReportStatusRequestDto request) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(
                postServicePort.changeReportStatus(id, request.getStatus(), request.getValidatorId())
        ));
    }
}
