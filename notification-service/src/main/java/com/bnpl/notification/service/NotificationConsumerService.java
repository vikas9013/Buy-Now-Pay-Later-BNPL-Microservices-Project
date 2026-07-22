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
        String msg = "\n----------------------------------------------------------------------\n" +
                     "📲 [SMS & EMAIL NOTIFICATION SENT]\n" +
                     "To User ID: " + event.getUserId() + "\n" +
                     "Message: 'Dear Customer, your BNPL checkout of ₹" + event.getAmount() + " at " + event.getMerchantName() + " was successful!'\n" +
                     "Transaction ID: " + event.getTransactionId() + "\n" +
                     "----------------------------------------------------------------------\n";
        System.out.println(msg);
        log.info(msg);
    }

    @KafkaListener(topics = "bnpl-repayment-events", groupId = "notification-service-group")
    public void handleRepaymentNotification(RepaymentEvent event) {
        String msg = "\n----------------------------------------------------------------------\n" +
                     "📲 [SMS & EMAIL NOTIFICATION SENT]\n" +
                     "To User ID: " + event.getUserId() + "\n" +
                     "Message: 'Dear Customer, thank you! Your BNPL repayment of ₹" + event.getAmount() + " was received.'\n" +
                     "Repayment ID: " + event.getRepaymentId() + "\n" +
                     "----------------------------------------------------------------------\n";
        System.out.println(msg);
        log.info(msg);
    }
}
