package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.infrastructure.out.jpa.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IUserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    Page<UserEntity> findAll(Pageable pageable);
}
