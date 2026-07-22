package com.bnpl.user.service;

import com.bnpl.user.dto.CreditAccountResponse;
import com.bnpl.user.dto.UserOnboardRequest;
import com.bnpl.user.entity.CreditAccount;
import com.bnpl.user.entity.User;
import com.bnpl.user.enums.AccountStatus;
import com.bnpl.user.enums.KycStatus;
import com.bnpl.user.repository.CreditAccountRepository;
import com.bnpl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CreditAccountRepository creditAccountRepository;

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("10000.00");

    @Transactional
    public User onboardUser(UserOnboardRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        });

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .kycStatus(KycStatus.APPROVED) // Defaulting to APPROVED for simplicity
                .build();

        User savedUser = userRepository.save(user);

        // Assign initial credit account with ₹10,000 limit
        CreditAccount account = CreditAccount.builder()
                .user(savedUser)
                .totalCreditLimit(DEFAULT_CREDIT_LIMIT)
                .availableCredit(DEFAULT_CREDIT_LIMIT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        creditAccountRepository.save(account);

        return savedUser;
    }

    public CreditAccountResponse getCreditAccountByUserId(UUID userId) {
        CreditAccount account = creditAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found for userId: " + userId));

        return mapToResponse(account);
    }

    @Transactional
    public CreditAccountResponse deductCredit(UUID userId, BigDecimal amount) {
        CreditAccount account = creditAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found for userId: " + userId));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active. Status: " + account.getAccountStatus());
        }

        if (account.getAvailableCredit().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient credit limit. Available: " + account.getAvailableCredit());
        }

        account.setAvailableCredit(account.getAvailableCredit().subtract(amount));
        CreditAccount updated = creditAccountRepository.save(account);

        return mapToResponse(updated);
    }

    @Transactional
    public CreditAccountResponse restoreCredit(UUID userId, BigDecimal amount) {
        CreditAccount account = creditAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found for userId: " + userId));

        account.setAvailableCredit(account.getAvailableCredit().add(amount));
        if (account.getAvailableCredit().compareTo(account.getTotalCreditLimit()) > 0) {
            account.setAvailableCredit(account.getTotalCreditLimit());
        }

        CreditAccount updated = creditAccountRepository.save(account);

        return mapToResponse(updated);
    }

    private CreditAccountResponse mapToResponse(CreditAccount account) {
        return CreditAccountResponse.builder()
                .accountId(account.getId())
                .userId(account.getUser().getId())
                .totalCreditLimit(account.getTotalCreditLimit())
                .availableCredit(account.getAvailableCredit())
                .accountStatus(account.getAccountStatus())
                .build();
    }
}
