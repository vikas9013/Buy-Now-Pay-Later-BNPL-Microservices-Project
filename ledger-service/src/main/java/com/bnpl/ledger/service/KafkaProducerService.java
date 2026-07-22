package com.bnpl.ledger.service;

import com.bnpl.ledger.event.CheckoutEvent;
import com.bnpl.ledger.event.RepaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String CHECKOUT_TOPIC = "bnpl-checkout-events";
    public static final String REPAYMENT_TOPIC = "bnpl-repayment-events";

    public void sendCheckoutEvent(CheckoutEvent event) {
        log.info("Publishing CheckoutEvent to Kafka topic '{}': {}", CHECKOUT_TOPIC, event);
        kafkaTemplate.send(CHECKOUT_TOPIC, event.getUserId().toString(), event);
    }

    public void sendRepaymentEvent(RepaymentEvent event) {
        log.info("Publishing RepaymentEvent to Kafka topic '{}': {}", REPAYMENT_TOPIC, event);
        kafkaTemplate.send(REPAYMENT_TOPIC, event.getUserId().toString(), event);
    }
}
