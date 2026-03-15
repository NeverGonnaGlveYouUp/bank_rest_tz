package com.example.bankcards.validators;

import com.example.bankcards.dto.UpdateUserAdminRequest;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mockito.internal.matchers.Equals;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.text.MessageFormat;

@Component
@RequiredArgsConstructor
public class AdminUserValidator implements Validator {

    private final static Integer minLoginLength = 8;

    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return UpdateUserAdminRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UpdateUserAdminRequest dto = (UpdateUserAdminRequest) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "newUsername", "newUsername.invalid.empty", "Логин обязателен.");
        if (dto.getNewUsername().length() < minLoginLength) {
            errors.rejectValue("newUsername", "newUsername.invalid.short", MessageFormat.format("Имя пользователя должно быть не менее {0} символов.", minLoginLength));
        }

        if (userRepository.existsByUsername(dto.getNewUsername())) {
            errors.rejectValue("newUsername", "newUsername.invalid.taken", "Имя пользователя уже занято.");
        }

        if (dto.getRoles() != null) {
            if (dto.getRoles().isEmpty()) {
                errors.rejectValue("roles", "roles.invalid.empty", "Список ролей не может быть пустым");
            }

            for (String role : dto.getRoles()) {
                if (role == null || role.isBlank()) {
                    errors.rejectValue("roles", "roles.invalid.", "Название роли не может быть пустым");
                    break;
                }
            }
        } else ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roles", "roles.invalid.empty", "Список ролей не может быть пустым.");
    }


}
