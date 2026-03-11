package com.example.bankcards.service;

import com.example.bankcards.dto.BalanceHistoryDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.revision.CustomRevisionEntity;
import com.example.bankcards.repository.CardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardAuditService {

    private final EntityManager entityManager;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public Page<BalanceHistoryDto> getBalanceHistory(Long cardId, int page, int size) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта не найдена"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !card.getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("Вы не можете просматривать историю чужой карты");
        }

        Long accountId = card.getCardAccount().getId();
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        Long totalCount = (Long) auditReader.createQuery()
                .forRevisionsOfEntity(Transfer.class, false, true)
                .add(AuditEntity.or(
                        AuditEntity.relatedId("fromAccount").eq(accountId),
                        AuditEntity.relatedId("toAccount").eq(accountId)
                ))
                .addProjection(AuditEntity.revisionNumber().count())
                .getSingleResult();

        List<Object[]> results = auditReader.createQuery()
                .forRevisionsOfEntity(Transfer.class, false, true)
                .add(AuditEntity.or(
                        AuditEntity.relatedId("fromAccount").eq(accountId),
                        AuditEntity.relatedId("toAccount").eq(accountId)
                ))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        List<BalanceHistoryDto> history = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Transfer transfer = (Transfer) row[0];
            CustomRevisionEntity rev = (CustomRevisionEntity) row[1];

            history.add(new BalanceHistoryDto(
                    transfer.getAmount().multiply(BigDecimal.valueOf(transfer.getFromAccount().getId().equals(accountId) ? -1 : 1)),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(rev.getTimestamp()), ZoneId.systemDefault()),
                    rev.getUsername(),
                    transfer.getStatus()
            ));
        }

        return new PageImpl<>(history, PageRequest.of(page, size), totalCount);
    }
}