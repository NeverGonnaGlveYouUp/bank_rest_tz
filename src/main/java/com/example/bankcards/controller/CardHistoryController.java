package com.example.bankcards.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.dto.BalanceHistoryDto;
import com.example.bankcards.service.CardAuditService;

/**
 * Контроллер для просмотра аудита и истории изменений баланса карт.
 * Предоставляет доступ к архивным данным о транзакциях и корректировках.
 */
@RestController
@RequestMapping("/api/cards-history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class CardHistoryController {

    private final CardAuditService auditService;

    /**
     * Получение постраничной истории изменения баланса конкретной карты.
     * <p>
     * Для пользователей с ролью USER внутри сервиса выполняется проверка владения картой.
     * Администраторы имеют доступ к истории любой карты.
     *
     * @param id   идентификатор карты
     * @param page номер страницы (начиная с 0)
     * @param size количество записей на одной странице
     * @return {@link ResponseEntity} со страницей истории баланса
     */
    @Operation(
            summary = "Получить историю баланса",
            description = "Возвращает список изменений баланса для указанной карты. " +
                    "Пользователи могут видеть только свои карты, администраторы — любые."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "История успешно получена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не владелец карты)"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
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