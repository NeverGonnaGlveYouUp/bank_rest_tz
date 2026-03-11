package com.example.bankcards.dto;

import com.example.bankcards.entity.Transfer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BalanceHistoryDto {
    private BigDecimal amountChanged;
    private LocalDateTime changeDate;
    private String changedBy;
    private Transfer.TransferStatus status;
}