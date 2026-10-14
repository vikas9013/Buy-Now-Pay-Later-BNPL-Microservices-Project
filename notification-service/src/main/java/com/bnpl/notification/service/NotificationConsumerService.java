package com.bnpl.notification.service;

import com.bnpl.notification.event.CheckoutEvent;
import com.bnpl.notification.event.RepaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerService {

    private final SmsService smsService;

    @Value("${notification.checkout-phone:+919968640964}")
    private String checkoutPhone = "+919968640964";

    @Value("${notification.repayment-phone:+919876543210}")
    private String repaymentPhone = "+919876543210";

    @KafkaListener(topics = "bnpl-checkout-events", groupId = "notification-service-group")
    public void handleCheckoutNotification(CheckoutEvent event) {
        String message = String.format(
                "Dear Customer, your BNPL checkout of ₹%s at %s was successful! Transaction ID: %s",
                event.getAmount(), event.getMerchantName(), event.getTransactionId()
        );

        log.info("----------------------------------------------------------------------");
        log.info("📲 [PROCESSING KAFKA CHECKOUT EVENT]");
        log.info(message);
        log.info("----------------------------------------------------------------------");

        smsService.sendSms(checkoutPhone, message);
    }

    @KafkaListener(topics = "bnpl-repayment-events", groupId = "notification-service-group")
    public void handleRepaymentNotification(RepaymentEvent event) {
        String message = String.format(
                "Dear Customer, thank you! Your BNPL repayment of ₹%s was received. Repayment ID: %s",
                event.getAmount(), event.getRepaymentId()
        );

        log.info("----------------------------------------------------------------------");
        log.info("📲 [PROCESSING KAFKA REPAYMENT EVENT]");
        log.info(message);
        log.info("----------------------------------------------------------------------");

        smsService.sendSms(repaymentPhone, message);
    }
}
