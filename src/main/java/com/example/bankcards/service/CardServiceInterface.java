package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

public interface CardServiceInterface {
    Page<CardDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    );

    @Transactional
    CardDto updateCard(
            Long cardId,
            UpdateCardRequest request
    ) throws AccessDeniedException;

    @Transactional
    void rollbackTransfer(
            Long transferId
    ) throws AccessDeniedException;

}
