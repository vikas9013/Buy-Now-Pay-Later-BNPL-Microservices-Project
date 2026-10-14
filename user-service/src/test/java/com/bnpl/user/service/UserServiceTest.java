package com.bnpl.user.service;

import com.bnpl.user.dto.AuthResponse;
import com.bnpl.user.dto.CreditAccountResponse;
import com.bnpl.user.dto.LoginRequest;
import com.bnpl.user.dto.RegisterRequest;
import com.bnpl.user.entity.CreditAccount;
import com.bnpl.user.entity.User;
import com.bnpl.user.enums.AccountStatus;
import com.bnpl.user.enums.KycStatus;
import com.bnpl.user.repository.CreditAccountRepository;
import com.bnpl.user.repository.UserRepository;
import com.bnpl.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditAccountRepository creditAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private CreditAccount sampleCreditAccount;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .fullName("Aarav Sharma")
                .email("aarav@example.com")
                .password("encoded_pass")
                .phoneNumber("+919876543210")
                .kycStatus(KycStatus.APPROVED)
                .build();

        sampleCreditAccount = CreditAccount.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .totalCreditLimit(new BigDecimal("10000.00"))
                .availableCredit(new BigDecimal("10000.00"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should successfully register user with default credit limit")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Aarav Sharma");
        request.setEmail("aarav@example.com");
        request.setPassword("plain_pass");
        request.setPhoneNumber("+919876543210");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain_pass")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(creditAccountRepository.save(any(CreditAccount.class))).thenReturn(sampleCreditAccount);
        when(jwtUtil.generateToken(sampleUser.getId(), sampleUser.getEmail())).thenReturn("mock_token");

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("Aarav Sharma", response.getFullName());
        assertEquals("mock_token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(creditAccountRepository, times(1)).save(any(CreditAccount.class));
    }

    @Test
    @DisplayName("Register should throw exception when email already exists")
    void testRegister_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Duplicate User");
        request.setEmail("aarav@example.com");
        request.setPassword("password");
        request.setPhoneNumber("+919876543211");

        when(userRepository.findByEmail("aarav@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("aarav@example.com");
        request.setPassword("correct_pass");

        when(userRepository.findByEmail("aarav@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correct_pass", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(sampleUser.getId(), sampleUser.getEmail())).thenReturn("token_123");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("token_123", response.getToken());
    }

    @Test
    @DisplayName("Login should throw exception with invalid password")
    void testLogin_InvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("aarav@example.com");
        request.setPassword("wrong_pass");

        when(userRepository.findByEmail("aarav@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Should deduct credit when sufficient balance exists")
    void testDeductCredit_Success() {
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));
        when(creditAccountRepository.save(any(CreditAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditAccountResponse response = userService.deductCredit(userId, new BigDecimal("3000.00"));

        assertNotNull(response);
        assertEquals(new BigDecimal("7000.00"), response.getAvailableCredit());
    }

    @Test
    @DisplayName("Deduct credit should throw exception when balance is insufficient")
    void testDeductCredit_InsufficientBalance() {
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));

        assertThrows(IllegalArgumentException.class, () -> {
            userService.deductCredit(userId, new BigDecimal("15000.00"));
        });
    }

    @Test
    @DisplayName("Should restore credit without exceeding total limit")
    void testRestoreCredit_Success() {
        sampleCreditAccount.setAvailableCredit(new BigDecimal("4000.00"));
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));
        when(creditAccountRepository.save(any(CreditAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditAccountResponse response = userService.restoreCredit(userId, new BigDecimal("2000.00"));

        assertNotNull(response);
        assertEquals(new BigDecimal("6000.00"), response.getAvailableCredit());
    }

    @Test
    @DisplayName("Should successfully upgrade total credit limit and increment available balance")
    void testUpgradeCreditLimit_Success() {
        sampleCreditAccount.setTotalCreditLimit(new BigDecimal("10000.00"));
        sampleCreditAccount.setAvailableCredit(new BigDecimal("4000.00"));
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));
        when(creditAccountRepository.save(any(CreditAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditAccountResponse response = userService.upgradeCreditLimit(userId, new BigDecimal("25000.00"), "Annual Income Review");

        assertNotNull(response);
        assertEquals(new BigDecimal("25000.00"), response.getTotalCreditLimit());
        assertEquals(new BigDecimal("19000.00"), response.getAvailableCredit());
    }

    @Test
    @DisplayName("Upgrade credit limit should throw exception when requested limit is not strictly greater than current limit")
    void testUpgradeCreditLimit_LowerOrEqualLimit_ThrowsException() {
        sampleCreditAccount.setTotalCreditLimit(new BigDecimal("10000.00"));
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));

        assertThrows(IllegalArgumentException.class, () -> {
            userService.upgradeCreditLimit(userId, new BigDecimal("8000.00"), "Invalid downgrade attempt");
        });
    }

    @Test
    @DisplayName("Upgrade credit limit should throw exception when account is not ACTIVE")
    void testUpgradeCreditLimit_InactiveAccount_ThrowsException() {
        sampleCreditAccount.setAccountStatus(AccountStatus.BLOCKED);
        when(creditAccountRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCreditAccount));

        assertThrows(IllegalStateException.class, () -> {
            userService.upgradeCreditLimit(userId, new BigDecimal("30000.00"), "Review");
        });
    }
}
