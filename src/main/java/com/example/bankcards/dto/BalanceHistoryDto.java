package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BalanceHistoryDto {
    private BigDecimal balance;
    private BigDecimal amountChanged;
    private String operationType;
    private LocalDateTime changeDate;
    private String changedBy;
}