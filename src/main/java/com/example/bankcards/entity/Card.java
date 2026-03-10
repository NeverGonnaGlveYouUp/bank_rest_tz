package com.example.bankcards.entity;

import com.example.bankcards.dto.CardDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.StringJoiner;

@Slf4j
@Getter
@Setter
@Entity
@Audited
@Table(name = "cards")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE cards SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @AuditJoinTable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @AuditJoinTable
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "card_acciount_id", referencedColumnName = "id")
    private CardAccount cardAccount;

    @Column(nullable = false)
    private String cardName;

    @ColumnTransformer(
            read = "pgp_sym_decrypt(card_number, 'app.encryption_key')",
            write = "pgp_sym_encrypt(?, 'app.encryption_key')")
    @Column(columnDefinition = "bytea", nullable = false)
    private String cardNumber;

    @Transient
    private String maskedNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public CardDto toCardDto() {
        if (maskedNumber == null) {
            maskCardNumber();
        }
        return new CardDto(
                id,
                maskedNumber,
                cardName,
                expiryDate,
                status
        );
    }

    @PostLoad
    private void maskCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 4) {
            this.maskedNumber = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        }
    }

    public enum CardStatus {
        ACTIVE, BLOCKED, EXPIRED, REQUEST_FOR_BLOCK
    }
}
