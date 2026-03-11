package com.example.bankcards.controller;

import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.validators.AuthDetailsValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Контроллер для аутентификации и регистрации пользователей.
 * Обеспечивает выдачу JWT токенов и создание новых учетных записей.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthDetailsValidator authDetailsValidator;

    /**
     * Аутентификация пользователя и выдача JWT токена.
     *
     * @param request объект с логином и паролем
     * @return {@link ResponseEntity} с JWT токеном или ошибками валидации
     */
    @Operation(summary = "Вход в систему", description = "Проверяет учетные данные и возвращает JWT токен.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный формат данных или ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
    })
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        log.info("User login attempt: {}", request.getUsername());

        Errors errors = new BeanPropertyBindingResult(request, "request");
        authDetailsValidator.validate(request, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateToken((UserDetails) authentication.getPrincipal());

        log.info("User {} has been successfully logged in.", request.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    /**
     * Регистрация нового пользователя с назначением ролей.
     *
     * @param request данные для регистрации (логин, пароль, список ролей)
     * @return сообщение об успешной регистрации
     */
    @Operation(summary = "Регистрация пользователя", description = "Создает новый аккаунт пользователя с указанными ролями.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пользователь уже существует"),
            @ApiResponse(responseCode = "500", description = "Указанная роль не найдена в базе данных")
    })
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        log.info("Starting registration of a new user: {}", request.getUsername());

        Errors errors = new BeanPropertyBindingResult(request, "request");
        authDetailsValidator.validate(request, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = request
                .getRoles()
                .stream()
                .map(roleRepository::findByName)
                .map(role -> role.orElseThrow(RuntimeException::new))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);

        log.info("User {} successfully created with roles: {}", user.getUsername(), request.getRoles());
        return ResponseEntity.ok("Пользователь успешно зарегистрировался!");
    }

}