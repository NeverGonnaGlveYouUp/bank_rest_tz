package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.service.AdminCardService;
import com.example.bankcards.util.ObjectToRsqlConverter;
import com.example.bankcards.validators.CardValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Контроллер для управления банковскими картами через панель администратора.
 * Позволяет создавать карты, искать их по фильтрам и управлять транзакциями.
 */
@Slf4j
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/card")
@RequiredArgsConstructor
public class AdminCardController {

    private final CardValidator cardValidator;
    private final AdminCardService adminCardService;

    /**
     * Создает новую банковскую карту для пользователя.
     *
     * @param createCardDto данные для создания карты
     * @return {@link ResponseEntity} с созданной картой или списком ошибок валидации
     */
    @Operation(summary = "Создать новую карту", description = "Создает карту для указанного пользователя с предварительной валидацией.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Карта успешно создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
    })
    @PostMapping("/create")
    public ResponseEntity<?> create(
            CreateCardDto createCardDto
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Administrator [{}] initiated the creation of a cardDto for user with ID: {}",
                adminUsername, createCardDto.getOwnerUserId());

        Errors errors = new BeanPropertyBindingResult(createCardDto, "createCardDto");
        cardValidator.validate(createCardDto, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        CardDto cardDto = adminCardService.createCard(createCardDto);

        URI location = MvcUriComponentsBuilder
                .fromMethodName(AdminCardController.class, "findAllByRsql", ObjectToRsqlConverter.toRsql(cardDto), "", null, null)
                .build()
                .encode()
                .toUri();
        log.info("The cardDto was created successfully. Admin: {}, Card ID: {}, Number: {}. Resource path: {}",
                adminUsername,
                cardDto.getId(), cardDto.getMaskedNumber(), location);
        return ResponseEntity.created(location).body(cardDto);
    }

    /**
     * Поиск карт с использованием RSQL фильтрации.
     *
     * @param search RSQL строка фильтрации (например, "status=='ACTIVE';cardAccount.balance>300.00")
     * @param sort   параметры сортировки
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список найденных карт
     */
    @Operation(summary = "Поиск карт по RSQL", description = "Гибкий поиск карт с использованием синтаксиса RSQL.")
    @GetMapping("/findAllByRsql")
    public ResponseEntity<?> findAllByRsql(
            @Parameter(description = "RSQL запрос для фильтрации (URL-encoded)")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Сортировка в формате: field,asc|desc")
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "Номер страницы")
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @Parameter(description = "Размер страницы")
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("findAllByRsql: user: {} search:{}, sort:{}, page:{}, size:{}",
                adminUsername, search, sort, page, size);
        if (search != null) search = UriUtils.decode(search, StandardCharsets.UTF_8);;
        return ResponseEntity.ok(adminCardService.findAllByRsql(search, sort, page, size));
    }

    /**
     * Частичное обновление данных карты.
     *
     * @param id      ID карты
     * @param request объект с обновляемыми полями (имя, статус)
     * @return обновленная карта
     */
    @Operation(summary = "Обновить параметры карты", description = "Изменение имени карты или статуса карты.")
    @PatchMapping("/{id}")
    public ResponseEntity<CardDto> updateCard(
            @PathVariable Long id,
            @RequestBody UpdateCardRequest request
    ) {
        CardDto updatedCard = adminCardService.updateCard(id, request);
        return ResponseEntity.ok(updatedCard);
    }

    /**
     * Откат перевода между картами. Восстанавливает балансы участников транзакции.
     *
     * @param id ID транзакции (перевода) для отката
     * @return сообщение об успешном завершении операции
     */
    @PostMapping("/transfers/{id}/rollback")
    public ResponseEntity<?> rollbackTransfer(
            @PathVariable Long id
    ) {
        adminCardService.rollbackTransfer(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Перевод успешно отменен, балансы восстановлены."));
    }

    /**
     * Удаление карты (мягкое удаление).
     * @param id ID карты
     */
    @Operation(
            summary = "Удалить карту",
            description = "Выполняет мягкое удаление карты. Карта перестает отображаться в списках, но сохраняется в базе данных."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Карта успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID карты") @PathVariable Long id
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Administrator [{}] requested deletion of card ID: {}", adminUsername, id);

        adminCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

}
