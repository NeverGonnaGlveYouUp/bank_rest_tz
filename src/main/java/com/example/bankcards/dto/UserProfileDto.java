package com.example.bankcards.dto;

import java.util.List;

public record UserProfileDto(
    UserDto user,
    List<UserAuditDto> auditHistory
) {}