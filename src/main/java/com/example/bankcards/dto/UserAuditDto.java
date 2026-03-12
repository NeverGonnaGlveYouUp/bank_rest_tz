package com.example.bankcards.dto;

import java.time.LocalDateTime;

public record UserAuditDto(
    Long rev,
    String revType,
    LocalDateTime timestamp,
    String changedBy
) {}