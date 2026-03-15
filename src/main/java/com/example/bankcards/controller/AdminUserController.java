package com.example.bankcards.controller;

import com.example.bankcards.dto.UpdateUserAdminRequest;
import com.example.bankcards.service.AdminUserService;
import com.example.bankcards.validators.AdminUserValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Контроллер администратора для управления учетными записями пользователей.
 * Предоставляет инструменты для поиска, фильтрации и мониторинга клиентской базы.
 */
@Slf4j
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@Tag(name = "Admin User Controller", description = "Управление пользователями (только для ADMIN)")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserValidator adminUserValidator;

    /**
     * Поиск пользователей в системе с использованием гибких RSQL фильтров.
     *
     * @param search RSQL запрос (например, "username=='john*';roles.name=='USER'")
     * @param sort   параметры сортировки (например, "createdAt,desc")
     * @param page   номер страницы (начиная с 0)
     * @param size   количество записей на странице
     * @return {@link ResponseEntity} со списком найденных пользователей и метаданными пагинации
     */
    @Operation(
            summary = "Поиск пользователей по RSQL",
            description = "Позволяет администратору искать пользователей по любым полям (имя, роль, статус) с поддержкой пагинации."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен: требуется роль ADMIN"),
            @ApiResponse(responseCode = "400", description = "Ошибка в синтаксисе RSQL запроса")
    })
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
        if (search != null) search = UriUtils.decode(search, StandardCharsets.UTF_8);
        return ResponseEntity.ok(adminUserService.findAllByRsql(search, sort, page, size));
    }

    /**
     * Обновление данных пользователя администратором.
     * Позволяет изменить имя пользователя и/или его роли.
     *
     * @param id      ID пользователя, данные которого нужно изменить
     * @param request объект с новым именем и списком ролей
     * @return подтверждение обновления
     */
    @Operation(
            summary = "Обновление данных пользователя",
            description = "Позволяет сменить username и назначить новый список ролей. " +
                    "Если поле не передано в JSON, оно не будет обновлено."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные пользователя успешно обновлены"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или роль не существует")
    })
    @PatchMapping("/{id}/details")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserAdminRequest request
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("updateUser: admin: {} target_id: {}, newUsername: {}, roles: {}",
                adminUsername, id, request.getNewUsername(), request.getRoles());

        Errors errors = new BeanPropertyBindingResult(request, "request");
        adminUserValidator.validate(request, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        adminUserService.updateUserByAdmin(id, request);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Данные пользователя успешно обновлены",
                "userId", id
        ));
    }

}
