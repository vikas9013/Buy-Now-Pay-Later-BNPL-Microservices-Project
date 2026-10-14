package com.bnpl.ledger.service;

import com.bnpl.ledger.client.UserServiceClient;
import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.FinancialAdvisorResponse;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
import com.bnpl.ledger.repository.RepaymentRepository;
import com.bnpl.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiFinancialAdvisorService {

    private final TransactionRepository transactionRepository;
    private final RepaymentRepository repaymentRepository;
    private final UserServiceClient userServiceClient;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:dummy-key-for-now}")
    private String openAiApiKey;

    public FinancialAdvisorResponse generateFinancialInsights(UUID userId) {
        log.info("🤖 Generating GenAI Financial Advisor Insights for Customer: {}", userId);

        // 1. Gather Historical Fintech Data
        CreditAccountResponse creditAccount = userServiceClient.getCreditAccount(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        List<Repayment> allRepayments = repaymentRepository.findByUserId(userId);

        BigDecimal totalLimit = creditAccount.getTotalCreditLimit() != null 
                ? creditAccount.getTotalCreditLimit() : new BigDecimal("10000.00");
        BigDecimal availableCredit = creditAccount.getAvailableCredit() != null 
                ? creditAccount.getAvailableCredit() : BigDecimal.ZERO;
        
        BigDecimal usedCredit = totalLimit.subtract(availableCredit);
        if (usedCredit.compareTo(BigDecimal.ZERO) < 0) {
            usedCredit = BigDecimal.ZERO;
        }

        BigDecimal utilizationPercent = BigDecimal.ZERO;
        if (totalLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercent = usedCredit.multiply(new BigDecimal("100"))
                    .divide(totalLimit, 1, RoundingMode.HALF_UP);
        }

        // 2. Repayment Statistics
        int completedCount = 0;
        int activeCount = 0;
        int overdueCount = 0;
        BigDecimal totalDebtOutstanding = BigDecimal.ZERO;

        for (Repayment r : allRepayments) {
            if (r.isPaid()) {
                completedCount++;
            } else {
                activeCount++;
                totalDebtOutstanding = totalDebtOutstanding.add(r.getAmountDue());
                if (r.getDueDate().isBefore(LocalDate.now())) {
                    overdueCount++;
                }
            }
        }

        double disciplineRatio = 100.0;
        if (!allRepayments.isEmpty()) {
            disciplineRatio = (completedCount * 100.0) / allRepayments.size();
        }

        // 3. Estimated Credit Score Calculation (300 to 850 scale)
        int estimatedScore = 750;
        if (utilizationPercent.compareTo(new BigDecimal("70.0")) > 0) {
            estimatedScore -= 60;
        } else if (utilizationPercent.compareTo(new BigDecimal("30.0")) <= 0) {
            estimatedScore += 30;
        }

        if (disciplineRatio >= 90.0) {
            estimatedScore += 35;
        } else if (disciplineRatio < 60.0) {
            estimatedScore -= 70;
        }

        estimatedScore -= (overdueCount * 40);
        if (estimatedScore > 850) estimatedScore = 850;
        if (estimatedScore < 300) estimatedScore = 300;

        String category;
        if (estimatedScore >= 780) category = "EXCELLENT";
        else if (estimatedScore >= 720) category = "GOOD";
        else if (estimatedScore >= 650) category = "MODERATE";
        else category = "AT_RISK";

        // 4. Generate AI Insights via Spring AI (or Fallback Engine)
        String executiveSummary;
        String riskAssessment;
        List<String> recommendations = new ArrayList<>();
        String engineModel = "Spring AI Intelligent Fintech Advisor (Deterministic Heuristics)";

        boolean openAiConfigured = openAiApiKey != null 
                && !openAiApiKey.isBlank() 
                && !openAiApiKey.contains("dummy");

        if (openAiConfigured && chatModel != null) {
            try {
                String prompt = String.format(
                        "You are an enterprise fintech credit risk advisor for a Buy Now Pay Later (BNPL) platform. " +
                        "Analyze this customer profile: " +
                        "Credit Limit: ₹%s, Available Credit: ₹%s, Utilization: %s%%, Total Transactions: %d, " +
                        "Active EMIs: %d, Repayment Discipline: %.1f%%, Estimated Score: %d (%s). " +
                        "Provide a concise JSON or 3-paragraph executive financial summary, risk rating, and 3 personalized budgeting tips.",
                        totalLimit, availableCredit, utilizationPercent, transactions.size(),
                        activeCount, disciplineRatio, estimatedScore, category
                );

                String aiResult = chatModel.call(prompt);
                executiveSummary = aiResult;
                riskAssessment = category.equals("EXCELLENT") || category.equals("GOOD")
                        ? "Low Default Probability (< 1.8%). High creditworthiness for limit increase."
                        : "Moderate-to-High Risk. Monitor upcoming installment due dates.";
                recommendations.add("Consider choosing 3-6 month tenures to maintain optimal monthly debt-to-income balance.");
                recommendations.add("Keep credit utilization below 30% to maximize bureau score enhancement.");
                recommendations.add("Setup UPI Auto-Pay for installments to ensure 100% on-time payment track record.");
                engineModel = "OpenAI GPT-4o via Spring AI";
            } catch (Exception ex) {
                log.warn("Spring AI live model invocation skipped or timed out: {}. Using deterministic analytics fallback.", ex.getMessage());
                executiveSummary = generateHeuristicSummary(category, utilizationPercent, disciplineRatio, activeCount);
                riskAssessment = generateRiskAssessment(category, overdueCount);
                recommendations = generateRecommendations(utilizationPercent, disciplineRatio, activeCount);
            }
        } else {
            executiveSummary = generateHeuristicSummary(category, utilizationPercent, disciplineRatio, activeCount);
            riskAssessment = generateRiskAssessment(category, overdueCount);
            recommendations = generateRecommendations(utilizationPercent, disciplineRatio, activeCount);
        }

        return FinancialAdvisorResponse.builder()
                .userId(userId)
                .creditScoreCategory(category)
                .estimatedCreditScore(estimatedScore)
                .totalCreditLimit(totalLimit)
                .availableCredit(availableCredit)
                .creditUtilizationPercent(utilizationPercent)
                .totalTransactionsCount(transactions.size())
                .activeInstallmentsCount(activeCount)
                .completedInstallmentsCount(completedCount)
                .totalDebtOutstanding(totalDebtOutstanding)
                .repaymentDisciplineRatio(disciplineRatio)
                .aiExecutiveSummary(executiveSummary)
                .riskAssessment(riskAssessment)
                .smartRecommendations(recommendations)
                .engineModel(engineModel)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private String generateHeuristicSummary(String category, BigDecimal utilization, double discipline, int activeEmis) {
        return String.format(
                "Customer exhibits an overall '%s' credit profile. Credit line utilization is currently at %s%% with %d active installment schedule(s). " +
                "Historical repayment discipline is maintained at %.1f%% across verified payment gateways.",
                category, utilization, activeEmis, discipline
        );
    }

    private String generateRiskAssessment(String category, int overdueCount) {
        if (overdueCount > 0) {
            return String.format("ELEVATED RISK: Account contains %d overdue installment(s). Late fee schedule enforced.", overdueCount);
        } else if ("EXCELLENT".equals(category) || "GOOD".equals(category)) {
            return "MINIMAL DEFAULT RISK (< 1.5%): Customer demonstrates high financial reliability and qualifies for credit line upgrade up to ₹25,000.";
        } else {
            return "MODERATE RISK: Debt servicing capacity is adequate, but higher credit utilization requires monitoring.";
        }
    }

    private List<String> generateRecommendations(BigDecimal utilization, double discipline, int activeEmis) {
        List<String> list = new ArrayList<>();
        if (utilization.compareTo(new BigDecimal("50.0")) > 0) {
            list.add("Pay down active installments to reduce credit utilization below 30% for improved credit score.");
        } else {
            list.add("Excellent credit utilization! You are eligible for 0% interest Pay-in-30 Days options.");
        }
        if (discipline < 80.0) {
            list.add("Enable UPI Auto-Debit to prevent delayed EMI payments and avoid compounding late penalties.");
        } else {
            list.add("100% on-time repayment track record. Consistent repayment strengthens bureau credit history.");
        }
        list.add("For purchases over ₹5,000, consider 6 to 9-month tenures to maintain balanced monthly cash flow.");
        return list;
    }
}
