package com.payflow.payflow.dto;

import com.payflow.payflow.model.User;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private User.Role role;
}