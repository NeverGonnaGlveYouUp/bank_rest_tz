package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.controller.util.Tools;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.UserCardService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WebMvcTest(UserCardController.class)
@DisplayName("Тесты для UserCardController")
@Import(SecurityConfig.class)
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserCardService userCardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl detailsService;
    private static final CurrentUser ADMIN =
            new CurrentUser("adminname", new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final CurrentUser USER =
            new CurrentUser("username", new SimpleGrantedAuthority("ROLE_USER"));
    private static final LocalDate CARD_DATE = LocalDate.now().plusYears(2);
    private CardDto cardDto;
    private CardDto cardDto2;

    @BeforeEach
    void setUp() {
        cardDto = new CardDto(
                1L,
                "1234****5678",
                "Test Card",
                CARD_DATE,
                Card.CardStatus.ACTIVE,
                BigDecimal.valueOf(1000.00)
        );
        cardDto2 = new CardDto(
                2L,
                "1234****5672",
                "Test Card 2",
                CARD_DATE,
                Card.CardStatus.ACTIVE,
                BigDecimal.valueOf(2000.00)
        );

        // Security mocks: JWT service to extract username from token
        when(jwtService.getUsernameFromToken(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            String subject = Tools.getUsernameFromTestToken(token);
            log.info("Test: user is '{}' in jwt token.", subject);
            return subject;
        });
    }

    @Test
    @DisplayName("findAllByRsql - Получить список карт без фильтрации")
    void findAllByRsql_NoFilter() throws Exception {
        // Подготовка данных
        List<CardDto> cards = Collections.singletonList(cardDto);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());
        when(userCardService.findAllByRsql(any(), any(), anyInt(), anyInt())).thenReturn(page);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(get("/api/user/findAllByRsql")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk());

        verify(userCardService, times(1)).findAllByRsql(null, null, 0, 10);

        // был проход по секьюрити
        validateSecurityStackTraversed();

        Assertions.assertNotNull(resultActions);
        resultActions
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("1234****5678"))
                .andExpect(jsonPath("$.content[0].cardName").value("Test Card"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].balance").value(1000.0))
                // pagination info
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    @DisplayName("findAllByRsql - Получить список карт с RSQL фильтром")
        // фильтр на самом деле не срабатывает, это в тесте сервиса проверяется
    void findAllByRsql_WithFilter() throws Exception {
        // Подготовка данных
        String searchParam = "balance>1000";
        List<CardDto> cards = Collections.singletonList(cardDto2);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());
        when(userCardService.findAllByRsql(anyString(), any(), anyInt(), anyInt())).thenReturn(page);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(get("/api/user/findAllByRsql")
                        .param("search", searchParam)
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk());

        // был проход по секьюрити
        validateSecurityStackTraversed();

        verify(userCardService, times(1)).findAllByRsql(searchParam, null, 0, 10);
    }

    @Test
    @DisplayName("findAllByRsql - Получить список карт с сортировкой")
    void findAllByRsql_WithSort() throws Exception {
        // Подготовка данных
        String sortParam = "id,desc";
        List<CardDto> cards = List.of(cardDto2, cardDto);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());
        when(userCardService.findAllByRsql(any(), anyString(), anyInt(), anyInt())).thenReturn(page);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(get("/api/user/findAllByRsql")
                        .param("sort", sortParam)
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("1234****5672"))
                .andExpect(jsonPath("$.content[0].cardName").value("Test Card 2"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].balance").value(2000.0))
                // pagination info
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.empty").value(false));

        verify(userCardService, times(1)).findAllByRsql(null, sortParam, 0, 10);
    }

    @Test
    @DisplayName("findAllByRsql - Запрет доступа для пользователя без роли USER")
    void findAllByRsql_Unauthorized() throws Exception {
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(get("/api/user/findAllByRsql")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login))
                )
                .andExpect(status().isForbidden());

        verify(userCardService, never()).findAllByRsql(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("transferMoney - Успешный перевод между картами")
    void transferMoney_Success() throws Exception {
        // Подготовка данных
        Long fromId = 1L;
        Long toId = 2L;
        BigDecimal amount = new BigDecimal("1000.50");
        doNothing().when(userCardService).transferMoney(fromId, toId, amount);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfer")
//                        .with(csrf())
                                .param("fromId", String.valueOf(fromId))
                                .param("toId", String.valueOf(toId))
                                .param("amount", amount.toString())
                                .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Перевод успешно выполнен"));

        verify(userCardService, times(1)).transferMoney(fromId, toId, amount);
    }

    @Test
    @DisplayName("transferMoney - Перевод с нулевой суммой")
    void transferMoney_ZeroAmount() throws Exception {
        // Подготовка данных
        Long fromId = 1L;
        Long toId = 2L;
        BigDecimal amount = BigDecimal.ZERO;
        doNothing().when(userCardService).transferMoney(fromId, toId, amount);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfer")
//                        .with(csrf())
                                .param("fromId", String.valueOf(fromId))
                                .param("toId", String.valueOf(toId))
                                .param("amount", amount.toString())
                                .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Перевод успешно выполнен"));

        verify(userCardService, times(1)).transferMoney(fromId, toId, amount);
    }

    @Test
    @DisplayName("transferMoney - Запрет доступа для пользователя без роли USER")
    void transferMoney_Unauthorized() throws Exception {
        // Подготовка данных
        Long fromId = 1L;
        Long toId = 2L;
        BigDecimal amount = new BigDecimal("1000.00");
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfer")
                        .param("fromId", String.valueOf(fromId))
                        .param("toId", String.valueOf(toId))
                        .param("amount", amount.toString())
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login)))
                .andExpect(status().isForbidden());

        verify(userCardService, never()).transferMoney(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("requestRollback - Успешный запрос на отмену перевода")
    void requestRollback_Success() throws Exception {
        // Подготовка данных
        Long transferId = 123L;
        doNothing().when(userCardService).rollbackTransfer(transferId);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);


        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfers/{id}/request-rollback", transferId)
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Запрос на отмену перевода успешно создан и ожидает проверки администратором"));

        verify(userCardService, times(1)).rollbackTransfer(transferId);
    }

    @Test
    @DisplayName("requestRollback - Запрос на отмену перевода с большим ID")
    void requestRollback_LargeId() throws Exception {
        // Подготовка данных
        Long transferId = 999_999_999_999_999_999L;
        doNothing().when(userCardService).rollbackTransfer(transferId);

        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfers/{id}/request-rollback", transferId)
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Запрос на отмену перевода успешно создан и ожидает проверки администратором"));

        verify(userCardService, times(1)).rollbackTransfer(transferId);
    }

    @Test
    @DisplayName("requestRollback - Запрет доступа для пользователя без роли USER")
    void requestRollback_Unauthorized() throws Exception {
        // Подготовка данных
        Long transferId = 123L;

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfers/{id}/request-rollback", transferId)
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login))
                )
                .andExpect(status().isForbidden());

        verify(userCardService, never()).rollbackTransfer(anyLong());
    }

    @Test
    @DisplayName("requestRollback - Запрет доступа для анонимного пользователя")
    void requestRollback_Anonymous() throws Exception {
        // Подготовка данных
        Long transferId = 123L;

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/user/transfers/{id}/request-rollback", transferId))
                .andExpect(status().isForbidden());

        verify(userCardService, never()).rollbackTransfer(anyLong());
    }


    private void mockUserDetailsForUser(CurrentUser currentTestUser) {
        UserDetails adminUserDetails = mock(UserDetails.class);
        when(adminUserDetails.getUsername()).thenReturn(currentTestUser.login);
        doReturn(List.of(currentTestUser.grantedAuthority)).when(adminUserDetails).getAuthorities();
        when(detailsService.loadUserByUsername(any())).thenReturn(adminUserDetails);
    }

    private void validateSecurityStackTraversed() {
        verify(jwtService).validateToken(any());
        verify(jwtService).getUsernameFromToken(any());
        verify(detailsService).loadUserByUsername(any());
    }

    private record CurrentUser(String login, GrantedAuthority grantedAuthority) {
    }
}