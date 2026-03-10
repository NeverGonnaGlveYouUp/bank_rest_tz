package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardFilterRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSort;
import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCardService implements CardServiceInterface {

    private final UserRepository userRepository;
    private final CardFilterRepository cardFilterRepository;

    @Override
    public Page<CardDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    ) {
        Long userId = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow().getId();
        search = search + ";user.id==" + userId;
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
