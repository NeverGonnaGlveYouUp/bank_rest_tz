package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Getter
@Setter
@Audited
@Entity
@Table(name = "cart_account")
public class CartAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "cartAccount")
    private Card card;

    @Column(nullable = false)
    private BigDecimal balance;
}