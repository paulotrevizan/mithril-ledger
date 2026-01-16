package com.trevizan.mithrilledger.repository;

import com.trevizan.mithrilledger.domain.Wallet;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

}
