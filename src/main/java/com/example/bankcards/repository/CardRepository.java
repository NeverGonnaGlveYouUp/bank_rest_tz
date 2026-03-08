package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findAllByUserId(Long userId, Pageable pageable);

    @Query(value = "SELECT * FROM cards", nativeQuery = true)
    List<Card> findAllIncludingDeleted();

    @Modifying
    @Transactional
    @Query(value = "UPDATE cards SET is_deleted = false WHERE id = ?", nativeQuery = true)
    void restoreById(Long id);
}
