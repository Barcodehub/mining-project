package com.mining.soft.infrastructure.input.mapper;

import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.infrastructure.input.dto.PostRequestDto;
import com.mining.soft.infrastructure.input.dto.PostResponseDto;
import com.mining.soft.infrastructure.input.dto.ReportResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IPostDtoMapper {
    Post toModel(PostRequestDto dto);
    Report toReportModel(PostRequestDto dto);
    PostResponseDto toResponseDto(Post post);
    ReportResponseDto toReportResponseDto(Report report);
    List<ReportResponseDto> toReportResponseDtoList(List<Report> reports);
}
