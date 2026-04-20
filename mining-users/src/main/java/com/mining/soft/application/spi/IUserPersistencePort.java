package com.mining.soft.application.spi;


import com.mining.soft.domain.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IUserPersistencePort {

    UserModel saveUser(UserModel user);

    Page<UserModel> findAll(Pageable pageable);

    Optional<UserModel> findById(Long id);
}
