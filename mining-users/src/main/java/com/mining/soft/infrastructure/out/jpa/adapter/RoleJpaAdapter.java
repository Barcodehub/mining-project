package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.application.spi.IRolePersistencePort;
import com.mining.soft.infrastructure.out.jpa.mapper.IRoleEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleJpaAdapter implements IRolePersistencePort {

    private final IRoleRepository roleRepository;
    private final IRoleEntityMapper roleEntityMapper;


    @Override
    public Optional<RoleModel> findById(Long id) {
        return roleRepository.findById(id)
                .map(roleEntityMapper::toDomain);
    }
}
