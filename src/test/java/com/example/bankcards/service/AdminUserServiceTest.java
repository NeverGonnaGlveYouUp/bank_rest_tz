package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserFilterRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserFilterRepository cardFilterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CardRepository cardRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                cardFilterRepository,
                cardRepository,
                userRepository,
                roleRepository
        );
    }

    @Test
    @DisplayName("findAllByRsql - успешное получение страницы пользователей")
    void findAllByRsql_ShouldReturnPageOfUserDtos() {
        // GIVEN (Дано)
        String search = "username==admin";
        String sort = "id,asc";
        int page = 0;
        int size = 10;

        // 1. Подготовка пользователя и ролей
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        User user = new User();
        user.setId(100L);
        user.setUsername("adminUser");
        user.setRoles(Set.of(adminRole));

        // 2. Подготовка карт пользователя
        Card card = new Card();
        card.setId(1L);
        card.setCardName("Main Card");
        card.setCardNumber("1111222233334444"); // Для метода toCardDto() в сущности

        Page<User> userPage = new PageImpl<>(List.of(user));

        // Мокаем поиск пользователей (любая спецификация, заданная страница)
        when(cardFilterRepository.findAll((Specification<User>) any(), any(Pageable.class))).thenReturn(userPage);

        // Мокаем поиск карт для этого пользователя
        when(cardRepository.findAllByUser(user)).thenReturn(List.of(card));

        // WHEN (Действие)
        Page<UserDto> result = adminUserService.findAllByRsql(search, sort, page, size);

        // THEN (Проверка)
        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        UserDto userDto = result.getContent().get(0);
        assertEquals(100L, userDto.getId());
        assertEquals("adminUser", userDto.getUsername());

        // Проверка ролей
        assertTrue(userDto.getRoles().contains("ROLE_ADMIN"));

        // Проверка вложенных карт
        assertEquals(1, userDto.getCards().size());
        assertEquals("Main Card", userDto.getCards().get(0).getCardName());

        // Верификация вызовов
        verify(cardFilterRepository).findAll((Specification<User>) any(), eq(PageRequest.of(page, size)));
        verify(cardRepository).findAllByUser(user);
    }

    @Test
    @DisplayName("findAllByRsql - возвращает пустую страницу, если пользователи не найдены")
    void findAllByRsql_ShouldReturnEmptyPage_WhenNoUsersFound() {
        // GIVEN
        when(cardFilterRepository.findAll((Specification<User>) any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // WHEN
        Page<UserDto> result = adminUserService.findAllByRsql(null, null, 0, 10);

        // THEN
        assertTrue(result.isEmpty());
        verify(cardRepository, never()).findAllByUser(any());
    }
}