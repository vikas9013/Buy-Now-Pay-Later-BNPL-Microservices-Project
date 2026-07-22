package com.bnpl.notification.service;

import com.bnpl.notification.event.CheckoutEvent;
import com.bnpl.notification.event.RepaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumerService {

    @KafkaListener(topics = "bnpl-checkout-events", groupId = "notification-service-group")
    public void handleCheckoutNotification(CheckoutEvent event) {
        log.info("----------------------------------------------------------------------");
        log.info("📲 [SMS & EMAIL NOTIFICATION SENT]");
        log.info("To User ID: {}", event.getUserId());
        log.info("Message: 'Dear Customer, your BNPL checkout of ₹{} at {} was successful!'", 
                event.getAmount(), event.getMerchantName());
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("----------------------------------------------------------------------");
    }

    @KafkaListener(topics = "bnpl-repayment-events", groupId = "notification-service-group")
    public void handleRepaymentNotification(RepaymentEvent event) {
        log.info("----------------------------------------------------------------------");
        log.info("📲 [SMS & EMAIL NOTIFICATION SENT]");
        log.info("To User ID: {}", event.getUserId());
        log.info("Message: 'Dear Customer, thank you! Your BNPL repayment of ₹{} was received.'", event.getAmount());
        log.info("Repayment ID: {}", event.getRepaymentId());
        log.info("----------------------------------------------------------------------");
    }
}
