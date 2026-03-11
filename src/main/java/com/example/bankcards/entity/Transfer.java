package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Audited
@Getter
@Setter
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotAudited
    @JoinColumn(name = "from_account_id")
    private CardAccount fromAccount;

    @ManyToOne
    @NotAudited
    @JoinColumn(name = "to_account_id")
    private CardAccount toAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.COMPLETED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransferStatus {
        COMPLETED, REQUEST_FOR_ROLLBACK, ROLLED_BACK
    }

}