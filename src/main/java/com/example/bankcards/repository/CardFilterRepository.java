package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardFilterRepository extends
        PagingAndSortingRepository<Card, Long>,
        JpaSpecificationExecutor<Card>,
        JpaRepository<Card, Long> {
}