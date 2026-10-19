package com.mining.soft.infrastructure.out.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mining.soft.domain.enums.PostType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_created_at", columnList = "createdAt"),
    @Index(name = "idx_post_lat_lng", columnList = "latitude, longitude"),
    @Index(name = "idx_post_type", columnList = "type"),
    @Index(name = "idx_post_cluster", columnList = "cluster_id")
})
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private PostType type;

    //  ubicación
    private Double latitude;
    private Double longitude;

    // Cluster geoespacial asignado (nullable)
    @Column(name = "cluster_id")
    private String clusterId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // relación con imágenes
    @BatchSize(size = 25)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImageEntity> images = new ArrayList<>();
}