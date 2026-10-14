package com.bnpl.user.service;

import com.bnpl.user.event.CheckoutEvent;
import com.bnpl.user.event.RepaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    @KafkaListener(topics = "bnpl-checkout-events", groupId = "user-service-group")
    public void consumeCheckoutEvent(CheckoutEvent event) {
        log.info("[KAFKA CONSUMER] Received CheckoutEvent from Kafka: userId={}, amount=₹{}, merchant={}",
                event.getUserId(), event.getAmount(), event.getMerchantName());
        // Custom event-driven handling or logging
    }

    @KafkaListener(topics = "bnpl-repayment-events", groupId = "user-service-group")
    public void consumeRepaymentEvent(RepaymentEvent event) {
        log.info("[KAFKA CONSUMER] Received RepaymentEvent from Kafka: userId={}, amount=₹{}",
                event.getUserId(), event.getAmount());
        // Custom event-driven handling or logging
    }
    // event-driven kafka consumer
}
