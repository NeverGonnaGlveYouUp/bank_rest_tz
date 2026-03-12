package com.example.bankcards.controller;

import com.example.bankcards.controller.util.Tools;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.AdminCardService;
import com.example.bankcards.validators.CardValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест контроллеров AdminCard (@WebMvcTest не используется)
 */
@ExtendWith(MockitoExtension.class)
class AdminCardControllerTest {

    @Mock
    private AdminCardService adminCardService;

    @Mock
    private CardValidator cardValidator;

    /**
     * Тестируемый сервис
     */
    @InjectMocks
    private AdminCardController adminCardController;

    private MockMvc mockMvc;

    private CardDto cardDto;
    private CreateCardDto createCardDto;
    private UpdateCardRequest updateCardRequest;

    private final String USER_NAME = "username";
    private final String ADMIN_NAME = "adminname";
    private final LocalDate CARD_DATE = LocalDate.now().plusYears(2);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCardController).build();

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

        updateCardRequest = new UpdateCardRequest();
        updateCardRequest.setCardName("Updated Card Name");
        updateCardRequest.setNewStatus(Card.CardStatus.ACTIVE);

    }

    @Test
    @DisplayName("Тест создания карты, положительный кейс")
    void create_WhenValidInput_ShouldReturnCreatedCard() throws Exception {
        // Подготовка данных
        com.example.bankcards.service.util.Tools.setSecCtx(ADMIN_NAME);

        when(adminCardService.createCard(any(CreateCardDto.class))).thenReturn(cardDto);
        doAnswer(invocation -> null)
                .when(cardValidator).validate(any(CreateCardDto.class), any(Errors.class));

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(post("/api/admin/card/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"ownerUserId\": 100,\n" +
                                "  \"expiryDate\": \"" + createCardDto.getExpiryDate() + "\"\n" +
                                "}")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN_NAME))
                )
                .andExpect(status().isCreated());
        Assertions.assertNotNull(resultActions);
        resultActions.andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedNumber").value("1234****5678"))
                .andExpect(jsonPath("$.cardName").value("Test Card"))
                .andExpect(jsonPath("$.expiryDate").isArray())
                .andExpect(jsonPath("$.expiryDate[0]").value(CARD_DATE.getYear()))
                .andExpect(jsonPath("$.expiryDate[1]").value(CARD_DATE.getMonthValue()))
                .andExpect(jsonPath("$.expiryDate[2]").value(CARD_DATE.getDayOfMonth()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("Тест создания карты, кейс невалидного ownerUserId")
    void create_WhenInvalidInput_ShouldReturnBadRequest() throws Exception {
        // Подготовка данных
        com.example.bankcards.service.util.Tools.setSecCtx(ADMIN_NAME);

        doAnswer(invocation -> {
            Errors errorsArg = invocation.getArgument(1);
            errorsArg.rejectValue("ownerUserId", "user.not.found", "Пользователь не существует");
            return null;
        }).when(cardValidator).validate(any(CreateCardDto.class), any(Errors.class));

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(post("/api/admin/card/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"ownerUserId\": -1,\"expiryDate\": \"%s\"}".formatted(createCardDto.getExpiryDate())
                        )
                        .header("Authorization", "Bearer " + Tools.generateTestToken(ADMIN_NAME))
                )
                .andExpect(status().isBadRequest());

        Assertions.assertNotNull(resultActions);
        resultActions.andExpect(jsonPath("$[0].field").value("ownerUserId"))
                .andExpect(jsonPath("$[0].defaultMessage").value("Пользователь не существует"))
                .andExpect(jsonPath("$[0].code").value("user.not.found"));
    }

    @DisplayName("Тест получения списка карт")
    @Test
    void findAllByRsql_WhenValidSearch_ShouldReturnCards() throws Exception {
        // Подготовка данных
        com.example.bankcards.service.util.Tools.setSecCtx(USER_NAME);

        List<CardDto> cards = Collections.singletonList(cardDto);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());
        when(adminCardService.findAllByRsql(anyString(), anyString(), anyInt(), anyInt())).thenReturn(page);

        // Выполнение метода и проверки
        ResultActions resultActions = mockMvc.perform(get("/api/admin/card/findAllByRsql")
                        .param("search", "status=='ACTIVE'")
                        .param("sort", "cardName,asc")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + Tools.generateTestToken(USER_NAME))
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

    @DisplayName("Тест обновления данных карты, удачный кейс")
    @Test
    void updateCard_WhenValidInput_ShouldReturnUpdatedCard() throws Exception {
        // Подготовка данных
        when(adminCardService.updateCard(anyLong(), any(UpdateCardRequest.class))).thenReturn(cardDto);

        // Выполнение метода и проверки
        mockMvc.perform(patch("/api/admin/card/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardName": "Updated Card Name",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardName").value("Test Card"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @DisplayName("Тест отката перевода, удачный кейс")
    @Test
    void rollbackTransfer_WhenValidId_ShouldReturnSuccessMessage() throws Exception {
        doNothing().when(adminCardService).rollbackTransfer(anyLong());

        mockMvc.perform(post("/api/admin/card/transfers/1/rollback"))
                .andExpect(status().isOk())
                // Проверяем поле "message" в JSON
                .andExpect(jsonPath("$.message").value("Перевод успешно отменен, балансы восстановлены."));
    }

    @DisplayName("Тест удаления карты, удачный кейс")
    @Test
    void delete_WhenValidId_ShouldReturnNoContent() throws Exception {
        // Подготовка данных
        doNothing().when(adminCardService).deleteCard(anyLong());

        // Выполнение метода и проверки
        mockMvc.perform(delete("/api/admin/card/1"))
                .andExpect(status().isNoContent());
    }
}