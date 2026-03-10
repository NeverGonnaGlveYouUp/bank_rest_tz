package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import org.springframework.data.domain.Page;

public interface CardServiceInterface {
    Page<CardDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    );
    CardDto editCard();
}
