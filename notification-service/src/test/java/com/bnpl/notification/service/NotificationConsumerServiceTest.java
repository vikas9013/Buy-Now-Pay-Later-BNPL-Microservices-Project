package com.bnpl.notification.service;

import com.bnpl.notification.event.CheckoutEvent;
import com.bnpl.notification.event.RepaymentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerServiceTest {

    @Mock
    private SmsService smsService;

    @InjectMocks
    private NotificationConsumerService notificationConsumerService;

    @Test
    @DisplayName("Should process checkout Kafka event and trigger SMS dispatch")
    void testHandleCheckoutNotification() {
        CheckoutEvent event = CheckoutEvent.builder()
                .transactionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("5000.00"))
                .merchantName("Apple Store")
                .timestamp(LocalDateTime.now())
                .build();

        notificationConsumerService.handleCheckoutNotification(event);

        verify(smsService, times(1)).sendSms(eq("+919968640964"), contains("Apple Store"));
    }

    @Test
    @DisplayName("Should process repayment Kafka event and trigger customer acknowledgment SMS")
    void testHandleRepaymentNotification() {
        RepaymentEvent event = RepaymentEvent.builder()
                .repaymentId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("2500.00"))
                .timestamp(LocalDateTime.now())
                .build();

        notificationConsumerService.handleRepaymentNotification(event);

        verify(smsService, times(1)).sendSms(eq("+919876543210"), contains("₹2500.00"));
    }
}
