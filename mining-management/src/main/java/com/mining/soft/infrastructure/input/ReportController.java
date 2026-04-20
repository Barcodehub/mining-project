package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.infrastructure.input.dto.ReportResponseDto;
import com.mining.soft.infrastructure.input.dto.ReportStatusRequestDto;
import com.mining.soft.infrastructure.input.mapper.IPostDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ReportResponseDto> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(postServicePort.getReportById(id)));
    }

    @PatchMapping("/{id}/request-validation")
    public ResponseEntity<ReportResponseDto> requestValidation(@PathVariable Long id) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(postServicePort.requestValidation(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReportResponseDto> changeStatus(@PathVariable Long id, @RequestBody ReportStatusRequestDto request) {
        return ResponseEntity.ok(postDtoMapper.toReportResponseDto(
                postServicePort.changeReportStatus(id, request.getStatus(), request.getValidatorId())
        ));
    }
}
