package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardAccount;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardAccountRepository;
import com.example.bankcards.repository.CardFilterRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberGenerator;
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
    public CardDto editCard() {
        return null;
    }
}
