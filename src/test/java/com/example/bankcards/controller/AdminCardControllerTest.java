package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.controller.util.Tools;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.AdminCardService;
import com.example.bankcards.validators.CardValidator;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.validation.Errors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест контроллеров AdminCard
 */
@WebMvcTest(AdminCardController.class)
@Import(SecurityConfig.class)
@Slf4j
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl detailsService;

    @MockBean
    private AdminCardService adminCardService;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private CardValidator cardValidator;

    private CardDto cardDto;

    private CreateCardDto createCardDto;

    private static final LocalDate CARD_DATE = LocalDate.now().plusYears(2);
    private static final CurrentUser ADMIN =
            new CurrentUser("adminname", new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final CurrentUser USER =
            new CurrentUser("username", new SimpleGrantedAuthority("ROLE_USER"));

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

        createCardDto = new CreateCardDto(
                100L,
                LocalDate.now().plusYears(3)
        );

        UpdateCardRequest updateCardRequest = new UpdateCardRequest();
        updateCardRequest.setCardName("Updated Card Name");
        updateCardRequest.setNewStatus(Card.CardStatus.ACTIVE);

        // Security mocks: JWT service to extract username from token
        when(jwtService.getUsernameFromToken(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            String subject = Tools.getUsernameFromTestToken(token);
            log.info("Test: user is '{}' in jwt token.", subject);
            return subject;
        });

    }

    @Test
    @DisplayName("Тест создания карты, положительный кейс")
    void create_By_Admin_WhenValidInput_ShouldReturnCreatedCard() throws Exception {
        // Подготовка данных
        when(adminCardService.createCard(any(CreateCardDto.class))).thenReturn(cardDto);
        doAnswer(invocation -> null)
                .when(cardValidator).validate(any(CreateCardDto.class), any(Errors.class));

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода
        ResultActions resultActions = mockMvc.perform(post("/api/admin/card/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\": 100,\"expiryDate\": \"%s\"}"
                                .formatted(createCardDto.getExpiryDate()))
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login))
                )
                .andExpect(status().isCreated());
        Assertions.assertNotNull(resultActions);

        // Проверки
        // был проход по секьюрити
        validateSecurityStackTraversed();
        // корректный возврат
        resultActions.andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedNumber").value("1234****5678"))
                .andExpect(jsonPath("$.cardName").value("Test Card"))
                .andExpect(jsonPath("$.expiryDate").value(CARD_DATE.format(DateTimeFormatter.ISO_DATE)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("Тест создания карты, положительный кейс, но с правами роли USER")
    void create_By_User__WhenValidInput_ShouldForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for USER user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода
        ResultActions resultActions = mockMvc.perform(post("/api/admin/card/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\": 100,\"expiryDate\": \"%s\"}"
                                .formatted(createCardDto.getExpiryDate()))
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isForbidden());
        Assertions.assertNotNull(resultActions);

        // Проверки
        // был проход по секьюрити
        validateSecurityStackTraversed();
        // корректный возврат при отказе доступа
        resultActions.andExpect(jsonPath("$.objectName").value("AccessDeniedException"));
    }

    @Test
    @DisplayName("Тест создания карты, кейс невалидного ownerUserId")
    void create_WhenInvalidInput_ShouldReturnBadRequest() throws Exception {
        // Подготовка данных
        doAnswer(invocation -> {
            Errors errorsArg = invocation.getArgument(1);
            errorsArg.rejectValue("ownerUserId", "user.not.found", "Пользователь не существует");
            return null;
        }).when(cardValidator).validate(any(CreateCardDto.class), any(Errors.class));

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(post("/api/admin/card/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"ownerUserId\": -1,\"expiryDate\": \"%s\"}".formatted(createCardDto.getExpiryDate())
                        )
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login))
                )
                .andExpect(status().isBadRequest());

        Assertions.assertNotNull(resultActions);
        resultActions.andExpect(jsonPath("$[0].field").value("ownerUserId"))
                .andExpect(jsonPath("$[0].defaultMessage").value("Пользователь не существует"))
                .andExpect(jsonPath("$[0].code").value("user.not.found"));
    }

    @DisplayName("Тест получения списка карт с ролью ADMIN")
    @Test
    void findAllByRsql_By_Admin_WhenValidSearch_ShouldReturnCards() throws Exception {
        // Подготовка данных
        List<CardDto> cards = Collections.singletonList(cardDto);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());
        when(adminCardService.findAllByRsql(anyString(), anyString(), anyInt(), anyInt())).thenReturn(page);

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(get("/api/admin/card/findAllByRsql")
                        .param("search", "status=='ACTIVE'")
                        .param("sort", "cardName,asc")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login))
                )
                .andExpect(status().isOk());

        Assertions.assertNotNull(resultActions);
        resultActions.andExpect(jsonPath("$.content[0].id").value(1))
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

    @DisplayName("Тест получения списка карт с ролью USER")
    @Test
    void findAllByRsql_By_User_WhenValidSearch_ShouldForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(get("/api/admin/card/findAllByRsql")
                        .param("search", "status=='ACTIVE'")
                        .param("sort", "cardName,asc")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login))
                )
                .andExpect(status().isForbidden());

        Assertions.assertNotNull(resultActions);
        // корректный возврат при отказе доступа
        resultActions.andExpect(jsonPath("$.objectName").value("AccessDeniedException"));

    }

    @DisplayName("Тест обновления данных карты, удачный кейс")
    @Test
    void updateCard_WhenValidInput_ShouldReturnUpdatedCard() throws Exception {
        // Подготовка данных
        when(adminCardService.updateCard(anyLong(), any(UpdateCardRequest.class))).thenReturn(cardDto);

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(patch("/api/admin/card/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardName": "Updated Card Name",
                                  "status": "ACTIVE"
                                }
                                """)
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardName").value("Test Card"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @DisplayName("Тест отката перевода, удачный кейс")
    @Test
    void rollbackTransfer_By_Admin_WhenValidId_ShouldReturnSuccessMessage() throws Exception {
        // Подготовка данных
        doNothing().when(adminCardService).rollbackTransfer(anyLong());

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/card/transfers/1/rollback")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login)))
                .andExpect(status().isOk())
                // Проверяем поле "message" в JSON
                .andExpect(jsonPath("$.message").value("Перевод успешно отменен, балансы восстановлены."));
    }

    @DisplayName("Тест отката перевода, с правами роли USER")
    @Test
    void rollbackTransfer_By_User_WhenValidId_ShouldReturnForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/card/transfers/1/rollback")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Тест удаления карты, удачный кейс")
    @Test
    void delete_By_Admin_WhenValidId_ShouldReturnNoContent() throws Exception {
        // Подготовка данных
        doNothing().when(adminCardService).deleteCard(anyLong());

        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login)))
                .andExpect(status().isNoContent());
    }

    @DisplayName("Тест удаления карты, с правами роли USER")
    @Test
    void delete_By_User_WhenValidId_ShouldForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(USER);
        // success validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER.login)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Тест удаления карты, кейс невалидного токена")
    @Test
    void delete_By_Admin_WhenValidId_ShouldForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(ADMIN);
        // fail validate any token
        when(jwtService.validateToken(anyString())).thenReturn(false);

        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN.login)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Тест удаления карты, кейс валидного токена пользователя с третьей ролью")
    @Test
    void delete_By_UnknownRole_WhenValidId_ShouldForbidden() throws Exception {
        // Подготовка данных
        // Подготовка секьюрити: мок UserDetailsService for ADMIN user
        mockUserDetailsForUser(new CurrentUser("ANYBODY", new SimpleGrantedAuthority("UNKNOWN")));
        // fail validate any token
        when(jwtService.validateToken(anyString())).thenReturn(true);

        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1")
                        .header("Authorization", "Bearer " + Tools.generateTestToken("ANYBODY")))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Тест удаления карты, без токена")
    @Test
    void delete_WithOutToken_WhenValidId_ShouldForbidden1() throws Exception {
        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1"))
                .andExpect(status().isForbidden());
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