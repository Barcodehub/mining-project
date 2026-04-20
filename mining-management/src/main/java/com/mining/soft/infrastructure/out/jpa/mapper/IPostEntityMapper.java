package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.domain.model.PostImage;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import com.mining.soft.infrastructure.out.jpa.entity.PostImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IPostEntityMapper {
    PostEntity toEntity(Post post);
    Post toModel(PostEntity postEntity);

    ReportEntity toReportEntity(Report report);
    Report toReportModel(ReportEntity reportEntity);

    List<Report> toReportModelList(List<ReportEntity> reportEntities);

    PostImageEntity toImageEntity(PostImage image);
    PostImage toImageModel(PostImageEntity imageEntity);
}
