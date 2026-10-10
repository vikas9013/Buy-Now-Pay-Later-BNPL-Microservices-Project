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
    @com.fasterxml.jackson.annotation.JsonProperty("isPaid")
    @Column(nullable = false)
    private boolean isPaid = false;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal lateFeeApplied = BigDecimal.ZERO;

    @Column(name = "payment_method")
    private String paymentMethod; // UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING

    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId; // e.g. pay_29QQo5c4GB2k...

    @Column(name = "gateway_order_id")
    private String gatewayOrderId; // e.g. order_9A33X564GB2k...

    @Column(name = "payment_gateway")
    private String paymentGateway; // RAZORPAY, STRIPE, INTERNAL

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;
}
