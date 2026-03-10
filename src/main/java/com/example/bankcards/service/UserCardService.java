package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardAccount;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSort;
import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCardService implements CardServiceInterface {

    private final UserRepository userRepository;
    private final CardFilterRepository cardFilterRepository;
    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;

    @Override
    public Page<CardDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        search = search + ";user.id==" + userId;
        Specification<Card> searchSpecification = toSpecification(search);
        Specification<Card> searchSpecificationSorted = searchSpecification.and(toSort(sort));
        return cardFilterRepository
                .findAll(searchSpecificationSorted, PageRequest.of(page, size))
                .map(Card::toCardDto);
    }

    @Override
    @Transactional
    public CardDto updateCard(Long cardId, UpdateCardRequest request) throws AccessDeniedException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Card card = cardRepository.findById(cardId)
                .filter(c -> c.getUser().getUsername().equals(username))
                .orElseThrow(() -> new AccessDeniedException("Карта не найдена или доступ запрещен"));

        if (request.getCardName() != null) {
            log.info("User {} changed card name to: {}", username, request.getCardName());
            card.setCardName(request.getCardName());
        }

        if (request.getNewStatus() != null) {
            if (request.getNewStatus() == Card.CardStatus.REQUEST_FOR_BLOCK) {
                card.setStatus(Card.CardStatus.REQUEST_FOR_BLOCK);
                log.info("User {} requested block for card {}", username, cardId);
            } else {
                throw new AccessDeniedException("Вы можете только запрашивать блокировку (REQUEST_FOR_BLOCK)");
            }
        }

        return cardRepository.save(card).toCardDto();
    }

    @Transactional
    public void transferMoney(Long fromCardId, Long toCardId, BigDecimal amount) throws AccessDeniedException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Сумма должна быть > 0");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Card fromCard = cardRepository.findById(fromCardId)
                .filter(c -> c.getUser().getUsername().equals(username))
                .orElseThrow(() -> new AccessDeniedException("Исходная карта не найдена или не ваша"));

        Card dreamingCard = cardRepository.findById(toCardId)
                .filter(c -> c.getUser().getUsername().equals(username))
                .orElseThrow(() -> new AccessDeniedException("Целевая карта не найдена или не ваша"));

        CardAccount fromAcc = fromCard.getCardAccount();
        CardAccount toAcc = dreamingCard.getCardAccount();

        if (fromAcc.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Недостаточно средств");

        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        toAcc.setBalance(toAcc.getBalance().add(amount));

        Transfer transfer = new Transfer();
        transfer.setFromAccount(fromAcc);
        transfer.setToAccount(toAcc);
        transfer.setAmount(amount);
        transferRepository.save(transfer);

        log.info("User {} transferred {} between his cards", username, amount);
    }

    @Override
    @Transactional
    public void rollbackTransfer(Long transferId) throws AccessDeniedException {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Перевод не найден"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        Long ownerId = transfer.getFromAccount().getCard().getUser().getId();

        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("Вы не можете запрашивать отмену чужого перевода");
        }

        if (transfer.getStatus() != Transfer.TransferStatus.COMPLETED) {
            throw new IllegalStateException("Запрос на отмену уже создан или перевод уже отменен");
        }

        transfer.setStatus(Transfer.TransferStatus.REQUEST_FOR_ROLLBACK);

    }

}
