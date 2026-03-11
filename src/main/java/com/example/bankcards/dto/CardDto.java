package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CardDto {
    private Long id;
    private String maskedNumber;
    private String cardName;
    private LocalDate expiryDate;
    private Card.CardStatus status;
    private BigDecimal balance;
}
