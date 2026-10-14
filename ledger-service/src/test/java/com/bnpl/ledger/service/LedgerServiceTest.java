package com.bnpl.ledger.service;

import com.bnpl.ledger.client.UserServiceClient;
import com.bnpl.ledger.dto.CheckoutRequest;
import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
import com.bnpl.ledger.enums.TransactionStatus;
import com.bnpl.ledger.repository.RepaymentRepository;
import com.bnpl.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @InjectMocks
    private LedgerService ledgerService;

    private UUID userId;
    private CreditAccountResponse activeCreditAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeCreditAccount = new CreditAccountResponse();
        activeCreditAccount.setUserId(userId);
        activeCreditAccount.setAccountStatus("ACTIVE");
        activeCreditAccount.setAvailableCredit(new BigDecimal("10000.00"));
        activeCreditAccount.setTotalCreditLimit(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Should successfully process 1-month checkout with 0% interest")
    void testProcessCheckout_1Month_ZeroInterest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("2000.00"));
        request.setMerchantName("Swiggy");
        request.setTenureMonths(1);

        Transaction savedTx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new BigDecimal("2000.00"))
                .merchantName("Swiggy")
                .transactionDate(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .build();

        when(userServiceClient.getCreditAccount(userId)).thenReturn(activeCreditAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        CheckoutResponse response = ledgerService.processCheckout(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getPrincipalAmount());
        assertEquals(BigDecimal.ZERO, response.getInterestRatePercent());
        assertEquals(new BigDecimal("2000.00"), response.getTotalAmountWithInterest());
        assertEquals(1, response.getTenureMonths());

        verify(userServiceClient, times(1)).deductCredit(userId, new BigDecimal("2000.00"));
        verify(repaymentRepository, times(1)).save(any(Repayment.class));
        verify(kafkaProducerService, times(1)).sendCheckoutEvent(any());
    }

    @Test
    @DisplayName("Should successfully process 3-month EMI checkout with 5% interest")
    void testProcessCheckout_3Months_5PercentInterest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("6000.00"));
        request.setMerchantName("Myntra");
        request.setTenureMonths(3);

        Transaction savedTx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new BigDecimal("6000.00"))
                .merchantName("Myntra")
                .transactionDate(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .build();

        when(userServiceClient.getCreditAccount(userId)).thenReturn(activeCreditAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        CheckoutResponse response = ledgerService.processCheckout(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("5.00"), response.getInterestRatePercent());
        assertEquals(new BigDecimal("6300.00"), response.getTotalAmountWithInterest());
        assertEquals(new BigDecimal("2100.00"), response.getMonthlyInstallment());
        assertEquals(3, response.getTenureMonths());

        verify(repaymentRepository, times(3)).save(any(Repayment.class));
    }

    @Test
    @DisplayName("Checkout should throw exception when credit is insufficient")
    void testProcessCheckout_InsufficientCredit() {
        activeCreditAccount.setAvailableCredit(new BigDecimal("500.00"));

        CheckoutRequest request = new CheckoutRequest();
        request.setUserId(userId);
        request.setAmount(new BigDecimal("2000.00"));
        request.setMerchantName("Amazon");
        request.setTenureMonths(1);

        when(userServiceClient.getCreditAccount(userId)).thenReturn(activeCreditAccount);

        assertThrows(IllegalArgumentException.class, () -> ledgerService.processCheckout(request));
        verify(userServiceClient, never()).deductCredit(any(), any());
    }

    @Test
    @DisplayName("Should successfully process installment repayment and restore credit")
    void testProcessRepayment_Success() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId)
                .userId(userId)
                .amount(new BigDecimal("2000.00"))
                .build();

        UUID repaymentId = UUID.randomUUID();
        Repayment repayment = Repayment.builder()
                .id(repaymentId)
                .userId(userId)
                .transaction(tx)
                .amountDue(new BigDecimal("2000.00"))
                .dueDate(LocalDate.now().plusMonths(1))
                .isPaid(false)
                .lateFeeApplied(BigDecimal.ZERO)
                .build();

        RepaymentRequest request = new RepaymentRequest();
        request.setRepaymentId(repaymentId);
        request.setAmount(new BigDecimal("2000.00"));

        when(repaymentRepository.findById(repaymentId)).thenReturn(Optional.of(repayment));
        when(repaymentRepository.save(any(Repayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repaymentRepository.findByTransactionId(txId)).thenReturn(List.of(repayment));

        Repayment result = ledgerService.processRepayment(request);

        assertNotNull(result);
        assertTrue(result.isPaid());
        verify(userServiceClient, times(1)).restoreCredit(eq(userId), any(BigDecimal.class));
        verify(kafkaProducerService, times(1)).sendRepaymentEvent(any());
    }
}
