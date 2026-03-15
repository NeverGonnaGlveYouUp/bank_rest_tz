package com.example.bankcards.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserAdminRequest {
    private String newUsername;
    private Set<String> roles;
}