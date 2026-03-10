package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import lombok.Data;

@Data
public class UpdateCardRequest {
    private String cardName;
    private Card.CardStatus newStatus;
}