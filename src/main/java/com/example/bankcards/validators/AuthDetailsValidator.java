package com.example.bankcards.validators;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.text.MessageFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthDetailsValidator implements Validator {

    private final static Integer minLoginLength = 8;
    private final static Integer minPasswordLength = 8;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return RegisterRequest.class.equals(clazz) || LoginRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof RegisterRequest request) {
            validateUsername(request.getUsername(), errors);
            validateUsernameUniqueness(request.getUsername(), errors);
            validatePassword(request.getPassword(), errors);
            validateRoles(request.getRoles(), errors);
        } else if (target instanceof LoginRequest request) {
            validateUsername(request.getUsername(), errors);
            validatePassword(request.getPassword(), errors);
        }
    }

    private void validateUsername(String username, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "login.invalid.empty", "Логин обязателен.");
        if (username.length() < minLoginLength) {
            errors.rejectValue("username", "username.invalid.short", MessageFormat.format("Имя пользователя должно быть не менее {0} символов.", minLoginLength));
        }
    }

    private void validateUsernameUniqueness(String username, Errors errors) {
        if (userRepository.existsByUsername(username)) {
            errors.rejectValue("username", "username.invalid.taken", "Имя пользователя уже занято.");
        }
    }

    private void validatePassword(String password, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "password.invalid.empty", "Пароль обязателен.");
        if (password.length() < 8) {
            errors.rejectValue("password", "password.invalid.short", MessageFormat.format("Пароль должен быть не менее {0} символов.", minPasswordLength));
        }
    }

    private void validateRoles(List<String> roles, Errors errors) {
        if (roles == null || roles.isEmpty()) {
            errors.rejectValue("roles", "roles.invalid.empty", "Роли не могут быть пустыми");
        }
        assert roles != null;
        for (String role : roles) {
            if (!roleRepository.existsByName(role)) {
                errors.rejectValue("roles", "roles.invalid.notfound", String.format("Роль '%s' не найдена", role));
            }
        }
    }

    public static Errors badCredentialsError(Errors errors){
        errors.rejectValue("password", "password.invalid");
        errors.rejectValue("username", "username.invalid");
        errors.reject("user.credentials.invalid", "Неверное имя пользователя или пароль");
        return errors;
    }

}
