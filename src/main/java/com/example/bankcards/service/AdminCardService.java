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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSort;
import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCardService implements CardServiceInterface {

    private final CardRepository cardRepository;
    private final CardFilterRepository cardFilterRepository;
    private final CardAccountRepository cardAccountRepository;
    private final UserRepository userRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final TransferRepository transferRepository;

    @Transactional
    public CardDto createCard(CreateCardDto createCardDto) {
        User cardOwner = userRepository.findById(createCardDto.getOwnerUserId()).orElseThrow();

        Card card = new Card();
        card.setUser(cardOwner);
        card.setCardNumber(cardNumberGenerator.generate());
        card.setCardName("Карта " + cardOwner.getUsername());
        card.setExpiryDate(createCardDto.getExpiryDate());
        card = cardRepository.save(card);

        CardAccount cardAccount = new CardAccount();
        cardAccount.setCard(card);
        cardAccountRepository.save(cardAccount);

        return card.toCardDto();
    }

    @Override
    public Page<CardDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    ) {
        Specification<Card> searchSpecification = toSpecification(search);
        Specification<Card> searchSpecificationSorted = searchSpecification.and(toSort(sort));
        return cardFilterRepository
                .findAll(searchSpecificationSorted, PageRequest.of(page, size))
                .map(Card::toCardDto);
    }

    @Override
    @Transactional
    public CardDto updateCard(Long cardId, UpdateCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта с ID " + cardId + " не найдена"));

        if (request.getCardName() != null) {
            card.setCardName(request.getCardName());
        }

        if (request.getNewStatus() != null) {
            log.info("Admin changed status of card {} to {}", cardId, request.getNewStatus());
            card.setStatus(request.getNewStatus());
        }

        return cardRepository.save(card).toCardDto();
    }

    @Override
    @Transactional
    public void rollbackTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow();

        if (transfer.getStatus() != Transfer.TransferStatus.REQUEST_FOR_ROLLBACK) {
            throw new IllegalStateException("Нет активного запроса на отмену этого перевода");
        }

        CardAccount fromAcc = transfer.getFromAccount();
        CardAccount toAcc = transfer.getToAccount();

        if (toAcc.getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new IllegalStateException("На целевом счету недостаточно средств для отката");
        }

        fromAcc.setBalance(fromAcc.getBalance().add(transfer.getAmount()));
        toAcc.setBalance(toAcc.getBalance().subtract(transfer.getAmount()));

        transfer.setStatus(Transfer.TransferStatus.ROLLED_BACK);

        log.info("The admin has cancelled the transfer ID: {}. Balances have been restored.", transferId);
    }
}
