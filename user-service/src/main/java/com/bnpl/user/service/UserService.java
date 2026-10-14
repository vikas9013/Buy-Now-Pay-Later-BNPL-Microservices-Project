package com.bnpl.user.service;

import com.bnpl.user.dto.AuthResponse;
import com.bnpl.user.dto.CreditAccountResponse;
import com.bnpl.user.dto.LoginRequest;
import com.bnpl.user.dto.RegisterRequest;
import com.bnpl.user.dto.UserOnboardRequest;
import com.bnpl.user.entity.CreditAccount;
import com.bnpl.user.entity.User;
import com.bnpl.user.enums.AccountStatus;
import com.bnpl.user.enums.KycStatus;
import com.bnpl.user.repository.CreditAccountRepository;
import com.bnpl.user.repository.UserRepository;
import com.bnpl.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedissonClient redissonClient;

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("10000.00");

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        });

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .kycStatus(KycStatus.APPROVED)
                .build();

        User savedUser = userRepository.save(user);

        CreditAccount account = CreditAccount.builder()
                .user(savedUser)
                .totalCreditLimit(DEFAULT_CREDIT_LIMIT)
                .availableCredit(DEFAULT_CREDIT_LIMIT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        creditAccountRepository.save(account);

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .token(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .token(token)
                .build();
    }

    @Transactional
    public User onboardUser(UserOnboardRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        });

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode("DefaultPassword123!"))
                .phoneNumber(request.getPhoneNumber())
                .kycStatus(KycStatus.APPROVED)
                .build();

        User savedUser = userRepository.save(user);

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
        String lockKey = "lock:credit:" + userId;
        RLock lock = (redissonClient != null) ? redissonClient.getLock(lockKey) : null;

        try {
            if (lock != null) {
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new IllegalStateException("Could not acquire lock for user credit account. Please try again.");
                }
                log.info("🔒 Redisson Lock acquired for user credit deduction: {}", lockKey);
            }

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Credit deduction interrupted", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Redisson Lock released: {}", lockKey);
            }
        }
    }

    @Transactional
    public CreditAccountResponse restoreCredit(UUID userId, BigDecimal amount) {
        String lockKey = "lock:credit:" + userId;
        RLock lock = (redissonClient != null) ? redissonClient.getLock(lockKey) : null;

        try {
            if (lock != null) {
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new IllegalStateException("Could not acquire lock for user credit account. Please try again.");
                }
                log.info("🔒 Redisson Lock acquired for user credit restoration: {}", lockKey);
            }

            CreditAccount account = creditAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Credit account not found for userId: " + userId));

            account.setAvailableCredit(account.getAvailableCredit().add(amount));
            if (account.getAvailableCredit().compareTo(account.getTotalCreditLimit()) > 0) {
                account.setAvailableCredit(account.getTotalCreditLimit());
            }

            CreditAccount updated = creditAccountRepository.save(account);

            return mapToResponse(updated);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Credit restoration interrupted", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Redisson Lock released: {}", lockKey);
            }
        }
    }

    @Transactional
    public CreditAccountResponse upgradeCreditLimit(UUID userId, BigDecimal newTotalLimit, String reason) {
        String lockKey = "lock:credit:" + userId;
        RLock lock = (redissonClient != null) ? redissonClient.getLock(lockKey) : null;

        try {
            if (lock != null) {
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new IllegalStateException("Could not acquire lock for user credit account upgrade. Please try again.");
                }
                log.info("🔒 Redisson Lock acquired for user credit upgrade: {}", lockKey);
            }

            CreditAccount account = creditAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Credit account not found for userId: " + userId));

            if (account.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalStateException("Cannot upgrade credit limit. Account is not active. Status: " + account.getAccountStatus());
            }

            if (newTotalLimit.compareTo(account.getTotalCreditLimit()) <= 0) {
                throw new IllegalArgumentException("New credit limit (₹" + newTotalLimit 
                        + ") must be strictly greater than current limit (₹" + account.getTotalCreditLimit() + ")");
            }

            BigDecimal limitDifference = newTotalLimit.subtract(account.getTotalCreditLimit());
            account.setTotalCreditLimit(newTotalLimit);
            account.setAvailableCredit(account.getAvailableCredit().add(limitDifference));

            CreditAccount updated = creditAccountRepository.save(account);
            log.info("🎉 [CREDIT UPGRADE SUCCESS] User: {}, Old Limit: ₹{}, New Limit: ₹{}, Reason: {}",
                    userId, account.getTotalCreditLimit().subtract(limitDifference), newTotalLimit, reason);

            return mapToResponse(updated);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Credit upgrade interrupted", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Redisson Lock released: {}", lockKey);
            }
        }
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
