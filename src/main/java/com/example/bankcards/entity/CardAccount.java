package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "card_account")
public class CardAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "cardAccount")
    private Card card;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CardAccount that = (CardAccount) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getCard(), that.getCard()) && Objects.equals(getBalance(), that.getBalance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCard(), getBalance());
    }
}