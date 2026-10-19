package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {

    private Long id;

    private String name;
    private String lastName;
    private String password;
    private String email;
    private String identificationDocument;
    private String phoneNumber;
    private LocalDate birthDate;
    private RoleModel role;
    private LocalDateTime createdAt;
    private UserModel createdBy;

}
