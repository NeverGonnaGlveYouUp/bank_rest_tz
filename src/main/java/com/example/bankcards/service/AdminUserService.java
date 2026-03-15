package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UpdateUserAdminRequest;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserFilterRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static io.github.perplexhub.rsql.RSQLJPASupport.toSort;
import static io.github.perplexhub.rsql.RSQLJPASupport.toSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserFilterRepository cardFilterRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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

    @Transactional
    public void updateUserByAdmin(
            Long id,
            UpdateUserAdminRequest request
    ) {
        User user = userRepository.findById(id).orElseThrow();

        if (request.getNewUsername() != null) {
            user.setUsername(request.getNewUsername());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = roleRepository.findByNameIn(request.getRoles());
            if (roles.isEmpty()) throw new EntityNotFoundException("Ни одной роли найдено не было.");
            user.setRoles(roles);
        }

    }

}
