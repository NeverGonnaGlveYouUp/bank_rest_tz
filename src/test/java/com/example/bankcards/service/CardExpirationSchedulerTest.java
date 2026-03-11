package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Тест сервиса CardExpirationScheduler
 */
@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    @Mock
    private CardRepository cardRepository;

    /**
     * Тестируемый сервис
     */
    @InjectMocks
    private CardExpirationScheduler cardExpirationScheduler;

    @Captor
    private ArgumentCaptor<List<Card>> cardListCaptor;

    @Test
    @DisplayName("Тест автоматической блокировки истекших карт")
    void testProcessExpiredCards_ShouldBlockExpiredCards() {
        // Подготовка данных
        LocalDate today = LocalDate.now();
        Card card1 = new Card();
        card1.setId(1L);
        card1.setStatus(Card.CardStatus.ACTIVE);
        card1.setExpiryDate(today.minusDays(1));

        Card card2 = new Card();
        card2.setId(2L);
        card2.setStatus(Card.CardStatus.ACTIVE);
        card2.setExpiryDate(today.minusDays(2));

        List<Card> expiredCards = List.of(card1, card2);

        // Моки
        when(cardRepository.findAllByStatusAndExpiryDateBefore(
                Card.CardStatus.ACTIVE, today))
                .thenReturn(expiredCards);

        // Выполнение метода
        cardExpirationScheduler.processExpiredCards();

        // Проверки
        verify(cardRepository).findAllByStatusAndExpiryDateBefore(
                Card.CardStatus.ACTIVE, today);

        verify(cardRepository).saveAll(cardListCaptor.capture());

        List<Card> capturedCards = cardListCaptor.getValue();
        assertEquals(2, capturedCards.size());
        assertTrue(capturedCards.stream().allMatch(card ->
                card.getStatus() == Card.CardStatus.EXPIRED));

    }

    @Test
    @DisplayName("Тест обработки истекших карт с картами, срок действия которых не истек")
    void testProcessExpiredCards_WithCardsNotExpired_ShouldNotProcessAnyCards() {
        // Подготовка данных
        LocalDate today = LocalDate.now();

        // Моки
        when(cardRepository.findAllByStatusAndExpiryDateBefore(
                Card.CardStatus.ACTIVE, today))
                .thenReturn(List.of());

        // Выполнение метода
        cardExpirationScheduler.processExpiredCards();

        // Проверки
        verify(cardRepository).findAllByStatusAndExpiryDateBefore(
                Card.CardStatus.ACTIVE, today);

        verify(cardRepository, never()).saveAll(any());

    }

}