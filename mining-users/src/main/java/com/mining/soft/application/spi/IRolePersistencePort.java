package com.mining.soft.application.spi;


import com.mining.soft.domain.model.RoleModel;

import java.util.Optional;

public interface IRolePersistencePort {
    Optional<RoleModel> findById(Long id);
}
