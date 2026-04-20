package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.domain.model.UserModel;
import com.mining.soft.application.spi.IUserPersistencePort;
import com.mining.soft.infrastructure.out.jpa.entity.UserEntity;
import com.mining.soft.infrastructure.out.jpa.mapper.IUserEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaAdapter implements IUserPersistencePort {

    private final IUserRepository userRepository;
    private final IUserEntityMapper userEntityMapper;


    @Override
    public UserModel saveUser(UserModel user) {
        UserEntity userEntity = userEntityMapper.toEntity(user);
        UserEntity savedEntity = userRepository.save(userEntity);
        return userEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Page<UserModel> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<UserModel> findById(Long id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toDomain);
    }
}
