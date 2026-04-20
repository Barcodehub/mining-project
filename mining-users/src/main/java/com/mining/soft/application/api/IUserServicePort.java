package com.mining.soft.application.api;

import com.mining.soft.domain.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface IUserServicePort {

    UserModel createUser(UserModel userModel);

    Page<UserModel> getAllUsers(Pageable pageable);

    UserModel getUserById(Long id);

}
