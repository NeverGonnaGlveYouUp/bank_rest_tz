package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardAccount;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.*;
import com.example.bankcards.util.CardNumberGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тест сервиса AdminCardService
 */
@ExtendWith(MockitoExtension.class)
class AdminCardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardFilterRepository cardFilterRepository;

    @Mock
    private CardAccountRepository cardAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private TransferRepository transferRepository;

    /**
     * Тестируемый сервис
     */
    private AdminCardService adminCardService;

    @BeforeEach
    void setUp() {
        adminCardService = new AdminCardService(
                cardRepository,
                cardFilterRepository,
                cardAccountRepository,
                userRepository,
                cardNumberGenerator,
                transferRepository
        );
    }

    @Test
    @DisplayName("Тест создания карты")
    void createCard_shouldCreateNewCardAndCardAccount() {
        // Подготовка данных
        Long userId = 1L;
        CreateCardDto createCardDto = new CreateCardDto(userId, LocalDate.now().plusYears(5));

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("testUser");

        Card mockCard = new Card();
        mockCard.setId(1L);
        mockCard.setCardNumber("1234567890123456");
        mockCard.setCardName("Карта testUser");
        mockCard.setExpiryDate(createCardDto.getExpiryDate());

        CardAccount mockCardAccount = new CardAccount();
        mockCardAccount.setId(1L);
        mockCardAccount.setBalance(BigDecimal.ZERO);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cardNumberGenerator.generate()).thenReturn("1234567890123456");
        when(cardRepository.save(any(Card.class))).thenReturn(mockCard);
        when(cardAccountRepository.save(any(CardAccount.class))).thenReturn(mockCardAccount);

        // Выполнение метода
        CardDto result = adminCardService.createCard(createCardDto);

        // Проверки
        assertNotNull(result);
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        assertEquals("Карта testUser", result.getCardName());
        assertEquals(createCardDto.getExpiryDate(), result.getExpiryDate());

        verify(userRepository).findById(userId);
        verify(cardNumberGenerator).generate();
        verify(cardRepository).save(any(Card.class));
        verify(cardAccountRepository).save(any(CardAccount.class));
    }

    @Test
    @DisplayName("Тест получения страницы карт")
    void findAllByRsql_shouldReturnPageOfCards() {
        // Подготовка данных
        String search = "cardName==*test*";
        String sort = "cardName,asc";
        Integer page = 0;
        Integer size = 10;

        Card mockCard = new Card();
        mockCard.setId(1L);
        mockCard.setCardNumber("1234567890123456");
        mockCard.setCardName("Карта testUser");

        List<Card> cardList = List.of(mockCard);
        Page<Card> mockPage = new PageImpl<>(cardList);

        when(cardFilterRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(mockPage);

        // Выполнение метода
        Page<CardDto> result = adminCardService.findAllByRsql(search, sort, page, size);

        // Проверки
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("**** **** **** 3456", result.getContent().get(0).getMaskedNumber());
        assertEquals("Карта testUser", result.getContent().get(0).getCardName());

        verify(cardFilterRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Обновления удачного обновленя данных карты")
    void updateCard_shouldUpdateCardNameAndStatus() {
        // Подготовка данных
        Long cardId = 1L;
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardName("Новое название карты");
        request.setNewStatus(Card.CardStatus.ACTIVE);

        Card existingCard = new Card();
        existingCard.setId(cardId);
        existingCard.setCardName("Старое название");
        existingCard.setStatus(Card.CardStatus.BLOCKED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(any(Card.class))).thenReturn(existingCard);

        // Выполнение метода
        CardDto result = adminCardService.updateCard(cardId, request);

        // Проверки
        assertNotNull(result);
        assertEquals("Новое название карты", result.getCardName());
        assertEquals(Card.CardStatus.ACTIVE, result.getStatus());

        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Тест обновленя данных карты - кейс когда карта не найдена")
    void updateCard_shouldThrowExceptionWhenCardNotFound() {
        // Подготовка данных
        Long cardId = 1L;
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardName("Новое название карты");

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // Выполнение метода и Проверки
        assertThrows(Exception.class,
                () -> adminCardService.updateCard(cardId, request),
                "Карта с ID 1 не найдена"
        );
        verify(cardRepository).findById(cardId);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Тест удачного кейса отката трансфера")
    void rollbackTransfer_shouldSuccessfullyRollbackTransfer() {
        // Подготовка данных
        Long transferId = 1L;
        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setStatus(Transfer.TransferStatus.REQUEST_FOR_ROLLBACK);
        transfer.setAmount(new BigDecimal("100.00"));

        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("200.00"));

        CardAccount toAccount = new CardAccount();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("300.00"));

        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Выполнение метода
        adminCardService.rollbackTransfer(transferId);

        // Проверки
        assertEquals(Transfer.TransferStatus.ROLLED_BACK, transfer.getStatus());
        assertEquals(new BigDecimal("300.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("200.00"), toAccount.getBalance());

        verify(transferRepository).findById(transferId);
        verify(transferRepository, never()).save(transfer);
    }

    @Test
    @DisplayName("Тест отката трансфера, когда нет активного запроса на отмену этого перевода")
    void rollbackTransfer_shouldThrowExceptionWhenTransferStatusIsNotRequestForRollback() {
        // Подготовка данных
        Long transferId = 1L;
        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setStatus(Transfer.TransferStatus.COMPLETED);
        transfer.setAmount(new BigDecimal("100.00"));

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Выполнение метода и Проверки
        assertThrows(IllegalStateException.class,
                () -> adminCardService.rollbackTransfer(transferId),
                "Нет активного запроса на отмену этого перевода");
        verify(transferRepository).findById(transferId);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Тест отката трансфера, когда на целевом счету недостаточно средств для отката")
    void rollbackTransfer_shouldThrowExceptionWhenInsufficientFunds() {
        // Подготовка данных
        Long transferId = 1L;
        Transfer transfer = getTransfer(transferId);

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Выполнение метода и Проверки
        assertThrows(IllegalStateException.class,
                () -> adminCardService.rollbackTransfer(transferId),
                "На целевом счету недостаточно средств для отката"
        );
        verify(transferRepository).findById(transferId);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Успешное мягкое удаление существующей карты")
    void deleteCard_Success() {
        // Подготовка данных
        Long cardId = 1L;
        Card card = new Card();
        card.setId(cardId);

        // Выполнение метода и Проверки
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        adminCardService.deleteCard(cardId);

        verify(cardRepository, times(1)).delete(card);
    }

    @Test
    @DisplayName("Ошибка при удалении несуществующей карты")
    void deleteCard_NotFound() {
        // Подготовка данных
        Long cardId = 99L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // Выполнение метода и Проверки
        assertThrows(NoSuchElementException.class, () -> adminCardService.deleteCard(cardId));
        verify(cardRepository, never()).delete(any());
    }

    private static @NonNull Transfer getTransfer(Long transferId) {
        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setStatus(Transfer.TransferStatus.REQUEST_FOR_ROLLBACK);
        transfer.setAmount(new BigDecimal("100.00"));

        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("200.00"));

        CardAccount toAccount = new CardAccount();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("50.00")); // не хватит

        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        return transfer;
    }

}