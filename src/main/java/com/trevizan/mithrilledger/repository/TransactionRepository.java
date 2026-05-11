package com.trevizan.mithrilledger.repository;

import com.trevizan.mithrilledger.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String key);

    @Query("select t from Transaction t where t.idempotencyKey = :key")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    Optional<Transaction> findByIdempotencyKeyInNewTransaction(@Param("key") String key);

}
