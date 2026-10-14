package com.bnpl.ledger.service;

import com.bnpl.ledger.client.UserServiceClient;
import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.FinancialAdvisorResponse;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
import com.bnpl.ledger.repository.RepaymentRepository;
import com.bnpl.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFinancialAdvisorServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AiFinancialAdvisorService aiFinancialAdvisorService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(aiFinancialAdvisorService, "openAiApiKey", "dummy-key-for-now");
    }

    @Test
    @DisplayName("Should successfully analyze customer profile and generate GenAI financial insights")
    void testGenerateFinancialInsights_Success() {
        CreditAccountResponse creditAccount = new CreditAccountResponse();
        creditAccount.setUserId(userId);
        creditAccount.setAccountStatus("ACTIVE");
        creditAccount.setTotalCreditLimit(new BigDecimal("10000.00"));
        creditAccount.setAvailableCredit(new BigDecimal("7000.00"));

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new BigDecimal("3000.00"))
                .merchantName("Myntra")
                .transactionDate(LocalDateTime.now())
                .build();

        Repayment paidRepayment = Repayment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amountDue(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().minusDays(5))
                .isPaid(true)
                .build();

        Repayment pendingRepayment = Repayment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amountDue(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusMonths(1))
                .isPaid(false)
                .build();

        when(userServiceClient.getCreditAccount(userId)).thenReturn(creditAccount);
        when(transactionRepository.findByUserId(userId)).thenReturn(List.of(tx));
        when(repaymentRepository.findByUserId(userId)).thenReturn(List.of(paidRepayment, pendingRepayment));

        FinancialAdvisorResponse response = aiFinancialAdvisorService.generateFinancialInsights(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertNotNull(response.getEstimatedCreditScore());
        assertTrue(response.getEstimatedCreditScore() >= 300 && response.getEstimatedCreditScore() <= 850);
        assertNotNull(response.getCreditScoreCategory());
        assertEquals(new BigDecimal("30.0"), response.getCreditUtilizationPercent());
        assertEquals(1, response.getActiveInstallmentsCount());
        assertEquals(1, response.getCompletedInstallmentsCount());
        assertEquals(new BigDecimal("1000.00"), response.getTotalDebtOutstanding());
        assertEquals(50.0, response.getRepaymentDisciplineRatio());
        assertNotNull(response.getAiExecutiveSummary());
        assertNotNull(response.getRiskAssessment());
        assertFalse(response.getSmartRecommendations().isEmpty());
    }
}
