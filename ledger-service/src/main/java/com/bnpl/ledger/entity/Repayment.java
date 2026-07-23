package com.bnpl.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private BigDecimal amountDue;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPaid = false;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal lateFeeApplied = BigDecimal.ZERO;
}
