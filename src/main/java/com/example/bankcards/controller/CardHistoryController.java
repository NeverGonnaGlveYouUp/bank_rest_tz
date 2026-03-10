package com.example.bankcards.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.dto.BalanceHistoryDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardAccountAuditService;

@RestController
@RequestMapping("/api/cards-history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class CardHistoryController {

    private final CardAccountAuditService auditService;

    /**
     * Получение истории баланса карты.
     * Доступно пользователям с ролями USER и ADMIN.
     * Логика проверки владения картой (для ROLE_USER) находится внутри сервиса.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Page<BalanceHistoryDto>> getHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<BalanceHistoryDto> history = auditService.getBalanceHistory(id, page, size);
        return ResponseEntity.ok(history);
    }
}