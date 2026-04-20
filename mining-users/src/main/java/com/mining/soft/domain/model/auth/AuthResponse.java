package com.mining.soft.domain.model.auth;

import com.mining.soft.domain.model.RoleModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private RoleModel role;

    public AuthResponse(String token, Long userId, String email, RoleModel role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }
}
