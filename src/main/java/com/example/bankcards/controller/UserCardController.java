package com.example.bankcards.controller;

import com.example.bankcards.service.UserCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCardController {

    private final UserCardService userCardService;

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
     * Перевод между своими картами.
     * @param fromId ID исходной карты
     * @param toId ID целевой карты
     * @param amount Сумма перевода
     */
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
     * Запрос на откат перевода (установка статуса REQUEST_FOR_ROLLBACK).
     * @param id ID перевода (Transfer)
     */
    @PostMapping("/transfers/{id}/request-rollback")
    public ResponseEntity<String> requestRollback(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Пользователь {} запросил отмену перевода ID: {}", username, id);

        userCardService.rollbackTransfer(id);
        return ResponseEntity.ok("Запрос на отмену перевода успешно создан и ожидает проверки администратором");
    }

}