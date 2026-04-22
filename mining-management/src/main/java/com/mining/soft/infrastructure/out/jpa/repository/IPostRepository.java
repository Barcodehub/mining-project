package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Page;


@Repository
public interface IPostRepository extends JpaRepository<PostEntity, Long> {

    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.images")
    Page<PostEntity> findAllWithImage(Pageable pageable);

}
