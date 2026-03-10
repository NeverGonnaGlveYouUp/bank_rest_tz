package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CreateCardDto {
    private Long ownerUserId;
    private LocalDate expiryDate;
}
