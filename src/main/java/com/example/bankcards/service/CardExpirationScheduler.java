package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void processExpiredCards() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "SYSTEM_SCHEDULER", null, Collections.emptyList()
        ));
        SecurityContextHolder.setContext(securityContext);

        try {
            LocalDate today = LocalDate.now();
            log.info("Launching the task to block expired cards: {}", today);

            List<Card> expiredCards = cardRepository.findAllByStatusAndExpiryDateBefore(
                    Card.CardStatus.ACTIVE, today);

            if (!expiredCards.isEmpty()) {
                expiredCards.forEach(card -> card.setStatus(Card.CardStatus.EXPIRED));
                cardRepository.saveAll(expiredCards);
                log.info("Automatically blocked cards: {}", expiredCards.size());
            }

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
