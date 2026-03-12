package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardAccount;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.*;
import com.example.bankcards.service.util.Tools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тест сервиса UserCardService
 */
@ExtendWith(MockitoExtension.class)
class UserCardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardFilterRepository cardFilterRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    /**
     * Тестируемый сервис
     */
    private UserCardService userCardService;

    @BeforeEach
    void setUp() {
        userCardService = new UserCardService(userRepository, cardFilterRepository, cardRepository, transferRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Тест метода findAllByRsql - успешное получение списка карт")
    void findAllByRsql_ShouldReturnPageOfCards() {
        // Подготовка данных
        String search = "cardName==test";
        String sort = "id,asc";
        Integer page = 0;
        Integer size = 10;
        String username = "testUser";
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        CardAccount cardAccount = new CardAccount();
        cardAccount.setId(1L);
        cardAccount.setBalance(new BigDecimal("1000.00"));

        Card card = new Card();
        card.setId(1L);
        card.setCardName("testCard");
        card.setMaskedNumber("1234****5678");
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setUser(user);
        card.setCardAccount(cardAccount);

        Page<Card> cardPage = new PageImpl<>(List.of(card));

        Tools.setSecCtx(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(cardFilterRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(cardPage);

        // Выполнение метода
        Page<CardDto> result = userCardService.findAllByRsql(search, sort, page, size);

        // Проверки
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testCard", result.getContent().get(0).getCardName());
        verify(userRepository).findByUsername(username);
        verify(cardFilterRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Тест метода updateCard - успешное обновление имени карты")
    void updateCard_ShouldUpdateCardName() throws AccessDeniedException {
        // Подготовка данных
        Long cardId = 1L;
        String username = "testUser";
        String newCardName = "Новое имя карты";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        Card card = new Card();
        card.setId(cardId);
        card.setCardName("Старое имя");
        card.setUser(user);

        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardName(newCardName);

        Tools.setSecCtx(username);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Выполнение метода
        CardDto result = userCardService.updateCard(cardId, request);

        // Проверки
        assertNotNull(result);
        assertEquals(newCardName, result.getCardName());
        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("Тест метода updateCard - успешное обновление статуса карты на REQUEST_FOR_BLOCK")
    void updateCard_ShouldUpdateCardStatusToRequestForBlock() throws AccessDeniedException {
        // Подготовка данных
        Long cardId = 1L;
        String username = "testUser";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        Card card = new Card();
        card.setId(cardId);
        card.setCardName("testCard");
        card.setUser(user);

        UpdateCardRequest request = new UpdateCardRequest();
        request.setNewStatus(Card.CardStatus.REQUEST_FOR_BLOCK);

        Tools.setSecCtx(username);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Выполнение метода
        CardDto result = userCardService.updateCard(cardId, request);

        // Проверки
        assertNotNull(result);
        assertEquals(Card.CardStatus.REQUEST_FOR_BLOCK, result.getStatus());
        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("Тест метода updateCard - исключение при попытке установить недопустимый статус")
    void updateCard_ShouldThrowAccessDeniedException_WhenInvalidStatus() {
        // Подготовка данных
        Long cardId = 1L;
        String username = "testUser";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        Card card = new Card();
        card.setId(cardId);
        card.setCardName("testCard");
        card.setUser(user);

        UpdateCardRequest request = new UpdateCardRequest();
        request.setNewStatus(Card.CardStatus.ACTIVE);

        Tools.setSecCtx(username);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        // Выполнение метода и Проверки
        assertThrows(AccessDeniedException.class, () -> userCardService.updateCard(cardId, request));
        verify(cardRepository).findById(cardId);
    }

    @Test
    @DisplayName("Тест метода transferMoney - успешный перевод между картами")
    void transferMoney_ShouldTransferMoneySuccessfully() throws AccessDeniedException {
        // Подготовка данных
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = new BigDecimal("1000.00");
        String username = "testUser";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("2000.00"));

        CardAccount toAccount = new CardAccount();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("1000.00"));

        Card fromCard = new Card();
        fromCard.setId(fromCardId);
        fromCard.setCardAccount(fromAccount);
        fromCard.setUser(user);

        Card toCard = new Card();
        toCard.setId(toCardId);
        toCard.setCardAccount(toAccount);
        toCard.setUser(user);

        Tools.setSecCtx(username);

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Выполнение метода
        userCardService.transferMoney(fromCardId, toCardId, amount);

        // Проверки
        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("2000.00"), toAccount.getBalance());
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Тест метода transferMoney - исключение при недостатке средств")
    void transferMoney_ShouldThrowAccessDeniedException_WhenInsufficientFunds() {
        // Подготовка данных
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = new BigDecimal("3000.00");
        String username = "testUser";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);

        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("2000.00"));

        CardAccount toAccount = new CardAccount();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("1000.00"));

        Card fromCard = new Card();
        fromCard.setId(fromCardId);
        fromCard.setCardAccount(fromAccount);
        fromCard.setUser(user);

        Card toCard = new Card();
        toCard.setId(toCardId);
        toCard.setCardAccount(toAccount);
        toCard.setUser(user);

        Tools.setSecCtx(username);

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        // Выполнение метода и Проверки
        assertThrows(AccessDeniedException.class, () -> userCardService.transferMoney(fromCardId, toCardId, amount));
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
    }

    @Test
    @DisplayName("Тест метода rollbackTransfer - успешная отмена перевода")
    void rollbackTransfer_ShouldRollbackTransferSuccessfully() throws AccessDeniedException {
        // Подготовка данных
        Long transferId = 1L;
        String username = "testUser";
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername(username);

        Card card = new Card();
        card.setUser(user);

        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setCard(card);

        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setStatus(Transfer.TransferStatus.COMPLETED);
        transfer.setFromAccount(fromAccount);

        Tools.setSecCtx(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Выполнение метода
        userCardService.rollbackTransfer(transferId);

        // Проверки
        assertEquals(Transfer.TransferStatus.REQUEST_FOR_ROLLBACK, transfer.getStatus());
        verify(transferRepository).findById(transferId);
        verify(transferRepository).save(transfer);
    }

    @Test
    @DisplayName("Тест метода rollbackTransfer - исключение при попытке отменить несуществующий перевод")
    void rollbackTransfer_ShouldThrowEntityNotFoundException_WhenTransferNotFound() {
        // Подготовка данных
        Long transferId = 1L;
        when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

        // Выполнение метода и Проверки
        assertThrows(Exception.class, () -> userCardService.rollbackTransfer(transferId));
        verify(transferRepository).findById(transferId);
    }

    @Test
    @DisplayName("Тест метода rollbackTransfer - исключение при попытке отменить перевод чужого пользователя")
    void rollbackTransfer_ShouldThrowAccessDeniedException_WhenTransferNotOwned() {
        // Подготовка данных
        Long transferId = 1L;

        String fromUsername = "testUser";
        Long fromUserId = 1L;
        User fromUser = new User();
        fromUser.setId(fromUserId);
        fromUser.setUsername(fromUsername);
        Card fromCard = new Card();
        fromCard.setUser(fromUser);
        CardAccount fromAccount = new CardAccount();
        fromAccount.setId(1L);
        fromAccount.setCard(fromCard);
        Transfer transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setStatus(Transfer.TransferStatus.COMPLETED);
        transfer.setFromAccount(fromAccount);

        String authenticatedUserName = "authTestUser";
        Long authUserId = 2L;
        User authUser = new User();
        fromUser.setId(authUserId);
        fromUser.setUsername(authenticatedUserName);

        Tools.setSecCtx(authenticatedUserName);

        when(userRepository.findByUsername(authenticatedUserName)).thenReturn(Optional.of(authUser));
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Выполнение метода и Проверки
        assertThrows(AccessDeniedException.class, () -> userCardService.rollbackTransfer(transferId));
        verify(transferRepository).findById(transferId);
    }
}