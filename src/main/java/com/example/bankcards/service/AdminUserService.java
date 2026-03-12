package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSort;
import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserFilterRepository cardFilterRepository;
    private final CardRepository cardRepository;

    public Page<UserDto> findAllByRsql(
            String search,
            String sort,
            Integer page,
            Integer size
    ) {
        Specification<User> searchSpecification = toSpecification(search);
        Specification<User> searchSpecificationSorted = searchSpecification.and(toSort(sort));
        return cardFilterRepository
                .findAll(searchSpecificationSorted, PageRequest.of(page, size))
                .map(user -> {
                    List<CardDto> cards = cardRepository.findAllByUser(user).stream().map(Card::toCardDto).toList();
                    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
                    return UserDto.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .roles(roles)
                            .cards(cards)
                            .build();
                });
    }

}
