package com.mining.soft.infrastructure.security.userdetails;

import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.infrastructure.out.jpa.entity.RoleEntity;
import com.mining.soft.infrastructure.out.jpa.entity.UserEntity;
import com.mining.soft.infrastructure.out.jpa.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        RoleEntity roleEntity = userEntity.getRole();
        RoleModel roleModel = new RoleModel();
        roleModel.setId(roleEntity.getId());
        roleModel.setName(roleEntity.getName());
        roleModel.setDescription(roleEntity.getDescription());

        return new CustomUserDetails(
                userEntity.getId(),
                userEntity.getEmail(),
                userEntity.getPassword(),
                roleModel
        );
    }
}
