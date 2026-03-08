package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

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

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cart_acciount_id", referencedColumnName = "id")
    private CartAccount cartAccount;

    @Column(nullable = false)
    private String cardName;

    @Column(nullable = false)
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
