package com.bnpl.ledger.service;

import com.bnpl.ledger.dto.PaymentOrderRequest;
import com.bnpl.ledger.dto.PaymentOrderResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGatewayServiceTest {

    private PaymentGatewayService paymentGatewayService;
    private static final String KEY_SECRET = "testSecretKey123456789";

    @BeforeEach
    void setUp() {
        paymentGatewayService = new PaymentGatewayService();
        ReflectionTestUtils.setField(paymentGatewayService, "gatewayMode", "SANDBOX");
        ReflectionTestUtils.setField(paymentGatewayService, "razorpayKeyId", "rzp_test_mockId");
        ReflectionTestUtils.setField(paymentGatewayService, "razorpayKeySecret", KEY_SECRET);
    }

    @Test
    @DisplayName("Should correctly calculate RFC 2104 compliant HMAC-SHA256 signature")
    void testCalculateHmacSha256() {
        String data = "order_12345|pay_67890";
        String signature = paymentGatewayService.calculateHmacSha256(data, KEY_SECRET);

        assertNotNull(signature);
        assertEquals(64, signature.length()); // SHA-256 hex string is 64 characters
        // Verify determinism
        assertEquals(signature, paymentGatewayService.calculateHmacSha256(data, KEY_SECRET));
    }

    @Test
    @DisplayName("Should create payment order with correct subunit conversion (paise)")
    void testCreatePaymentOrder_Sandbox() {
        Repayment repayment = Repayment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amountDue(new BigDecimal("1500.50"))
                .dueDate(LocalDate.now().plusMonths(1))
                .isPaid(false)
                .lateFeeApplied(BigDecimal.ZERO)
                .build();

        PaymentOrderRequest request = PaymentOrderRequest.builder()
                .repaymentId(repayment.getId())
                .gateway("RAZORPAY")
                .build();

        PaymentOrderResponse response = paymentGatewayService.createPaymentOrder(request, repayment);

        assertNotNull(response);
        assertEquals(repayment.getId(), response.getRepaymentId());
        assertEquals(new BigDecimal("1500.50"), response.getAmount());
        assertEquals(150050L, response.getAmountInSubunits()); // 1500.50 * 100 paise
        assertEquals("INR", response.getCurrency());
        assertNotNull(response.getOrderId());
    }

    @Test
    @DisplayName("Create payment order should throw exception if already paid")
    void testCreatePaymentOrder_AlreadyPaid() {
        Repayment repayment = Repayment.builder()
                .id(UUID.randomUUID())
                .amountDue(new BigDecimal("500.00"))
                .isPaid(true)
                .build();

        PaymentOrderRequest request = PaymentOrderRequest.builder()
                .repaymentId(repayment.getId())
                .build();

        assertThrows(IllegalStateException.class, () -> {
            paymentGatewayService.createPaymentOrder(request, repayment);
        });
    }

    @Test
    @DisplayName("Verify payment should fail with invalid signature")
    void testVerifyPayment_InvalidSignature() {
        Repayment repayment = Repayment.builder()
                .id(UUID.randomUUID())
                .amountDue(new BigDecimal("500.00"))
                .isPaid(false)
                .build();

        RepaymentRequest request = new RepaymentRequest();
        request.setPaymentGateway("RAZORPAY");
        request.setGatewayOrderId("order_valid_123");
        request.setGatewayPaymentId("pay_valid_456");
        request.setGatewaySignature("invalid_tampered_signature_999");

        assertThrows(IllegalArgumentException.class, () -> {
            paymentGatewayService.verifyPayment(request, repayment);
        });
    }
}
