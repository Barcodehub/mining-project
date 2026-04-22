package com.mining.soft.infrastructure.out.jpa.mapper;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.domain.model.PostImage;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import com.mining.soft.infrastructure.out.jpa.entity.PostImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IPostEntityMapper {
    default PostEntity toEntity(Post post) {
        if (post instanceof Report report) {
            return toReportEntity(report);
        }
        return toPostEntity(post);
    }
    default Post toModel(PostEntity postEntity) {
        if (postEntity instanceof ReportEntity reportEntity) {
            return toReportModel(reportEntity);
        }
        return toPostModel(postEntity);
    }
    PostEntity toPostEntity(Post post);
    Post toPostModel(PostEntity postEntity);
    ReportEntity toReportEntity(Report report);
    Report toReportModel(ReportEntity reportEntity);

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "post.images", ignore = true)
    List<Report> toReportModelList(List<ReportEntity> reportEntities);

    PostImageEntity toImageEntity(PostImage image);
    PostImage toImageModel(PostImageEntity imageEntity);
}
