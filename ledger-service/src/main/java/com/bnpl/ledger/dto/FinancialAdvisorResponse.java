package com.bnpl.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAdvisorResponse {
    private UUID userId;
    private String creditScoreCategory; // EXCELLENT (780+), GOOD (720-779), MODERATE (650-719), AT_RISK (<650)
    private Integer estimatedCreditScore;
    private BigDecimal totalCreditLimit;
    private BigDecimal availableCredit;
    private BigDecimal creditUtilizationPercent;
    private Integer totalTransactionsCount;
    private Integer activeInstallmentsCount;
    private Integer completedInstallmentsCount;
    private BigDecimal totalDebtOutstanding;
    private Double repaymentDisciplineRatio; // e.g. 100.0%
    private String aiExecutiveSummary;
    private String riskAssessment;
    private List<String> smartRecommendations;
    private String engineModel; // "OpenAI GPT-4o via Spring AI" or "Deterministic Fintech Heuristic Engine (Offline Fallback)"
    private LocalDateTime generatedAt;
}
