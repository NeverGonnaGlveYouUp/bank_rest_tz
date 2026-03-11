package com.example.bankcards.controller;

import com.example.bankcards.service.UserCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Контроллер для работы пользователя со своими банковскими картами.
 * Позволяет просматривать список карт, совершать переводы и запрашивать их отмену.
 */
@Slf4j
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCardController {

    private final UserCardService userCardService;

    /**
     * Получение списка карт пользователя с поддержкой фильтрации RSQL.
     *
     * @param search RSQL фильтр (например, "balance>100")
     * @param sort   параметры сортировки (например, "id,desc")
     * @param page   номер страницы
     * @param size   количество элементов на странице
     * @return список карт, соответствующих фильтру
     */
    @Operation(summary = "Получить свои карты", description = "Возвращает список карт текущего пользователя с возможностью фильтрации и пагинации.")
    @GetMapping("/findAllByRsql")
    public ResponseEntity<?> findAllByRsql(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("findAllByRsql: user: {} search:{}, sort:{}, page:{}, size:{}",
                adminUsername, search, sort, page, size);
        if (search != null) search = UriUtils.decode(search, StandardCharsets.UTF_8);
        return ResponseEntity.ok(userCardService.findAllByRsql(search, sort, page, size));
    }

    /**
     * Перевод между картами пользователя.
     *
     * @param fromId ID карты-отправителя
     * @param toId   ID карты-получателя
     * @param amount сумма перевода
     * @return текстовое сообщение о результате операции
     */
    @Operation(summary = "Перевод между картами", description = "Осуществляет перевод средств между двумя картами, принадлежащими пользователю.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен"),
            @ApiResponse(responseCode = "403", description = "Попытка доступа к чужой карте")
    })
    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(
            @RequestParam Long fromId,
            @RequestParam Long toId,
            @RequestParam BigDecimal amount) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Пользователь {} инициировал перевод {} с карты {} на карту {}", username, amount, fromId, toId);

        userCardService.transferMoney(fromId, toId, amount);
        return ResponseEntity.ok("Перевод успешно выполнен");
    }

    /**
     * Создание запроса на отмену (откат) ранее совершенного перевода.
     *
     * @param id ID транзакции перевода
     * @return текстовое сообщение о создании запроса
     */
    @Operation(summary = "Запросить отмену перевода", description = "Устанавливает статус REQUEST_FOR_ROLLBACK для транзакции. Окончательное решение принимает администратор.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос на откат успешно создан"),
            @ApiResponse(responseCode = "404", description = "Перевод с таким ID не найден")
    })
    @PostMapping("/transfers/{id}/request-rollback")
    public ResponseEntity<String> requestRollback(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Пользователь {} запросил отмену перевода ID: {}", username, id);

        userCardService.rollbackTransfer(id);
        return ResponseEntity.ok("Запрос на отмену перевода успешно создан и ожидает проверки администратором");
    }

}