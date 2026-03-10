package com.example.bankcards.repository;

import com.example.bankcards.entity.CardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAccountRepository extends JpaRepository<CardAccount, Long> {
}
