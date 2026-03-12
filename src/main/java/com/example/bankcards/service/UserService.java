package com.example.bankcards.service;

import com.example.bankcards.dto.UserAuditDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.UserProfileDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.revision.CustomRevisionEntity;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileWithHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Object[]> results = reader.createQuery()
                .forRevisionsOfEntity(User.class, false, true)
                .add(AuditEntity.id().eq(user.getId()))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        List<UserAuditDto> auditHistory = results.stream().map(row -> {
            CustomRevisionEntity rev = (CustomRevisionEntity) row[1];
            RevisionType type = (RevisionType) row[2];

            return new UserAuditDto(
                    rev.getId(),
                    type.name(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(rev.getTimestamp()), ZoneId.systemDefault()),
                    rev.getUsername()
            );
        }).toList();

        return new UserProfileDto(
                new UserDto(
                        user.getId(),
                        user.getUsername(),
                        null,
                        user.getRoles().stream().map(Role::getName).toList()),
                auditHistory);
    }
}