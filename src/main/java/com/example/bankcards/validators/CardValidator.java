package com.example.bankcards.validators;

import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.DateTimeException;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CardValidator implements Validator {

    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateCardDto.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CreateCardDto dto = (CreateCardDto) target;

        if (dto.getOwnerUserId() != null) {
            if (!userRepository.existsById(dto.getOwnerUserId())) {
                errors.rejectValue("ownerUserId", "user.not.found", "Пользователь не существует");
            }
        } else {
            errors.rejectValue("ownerUserId", "field.required", "Требуется Id владельца.");
        }

        if (dto.getExpiryDate() == null) {
            errors.rejectValue("expiryDate", "field.required", "Требуется указать срок действия.");
        } else {
            try {
                LocalDate expiryDate = dto.getExpiryDate();
                if (!expiryDate.isAfter(LocalDate.now())) {
                    errors.rejectValue("expiryDate", "date.past", "Срок действия должен быть в будущем.");
                }
                if (expiryDate.isAfter(LocalDate.now().plusYears(10))) {
                    errors.rejectValue("expiryDate", "date.too.far", "Срок действия не может превышать 10 лет.");
                }
            } catch (DateTimeException e) {
                errors.rejectValue("expiryDate", "date.invalid", "Некорректный формат даты.");
            }
        }
    }

    public static Errors accessDeniedException(Errors errors, String message) {
        errors.reject("access.denied", message != null ? message : "Доступ запрещен");
        return errors;
    }
}
