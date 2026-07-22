package com.bnpl.user.repository;

import com.bnpl.user.entity.CreditAccount;
import com.bnpl.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditAccountRepository extends JpaRepository<CreditAccount, UUID> {
    Optional<CreditAccount> findByUser(User user);
    Optional<CreditAccount> findByUserId(UUID userId);
}
