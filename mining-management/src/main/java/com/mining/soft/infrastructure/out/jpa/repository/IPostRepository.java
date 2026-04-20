package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPostRepository extends JpaRepository<PostEntity, Long> {
}
