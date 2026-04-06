package com.fraud.kafka.consumer;

import com.fraud.fraudcase.service.FraudCaseService;
import com.fraud.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final FraudCaseService fraudCaseService;

    @KafkaListener(topics = "payment-transactions", groupId = "fraud-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        log.info("Received transaction event for fraud analysis: {} amount: {}",
                event.getTransactionId(), event.getAmount());
        try {
            fraudCaseService.processTransaction(event);
        } catch (Exception ex) {
            log.error("Error processing transaction for fraud: {}", event.getTransactionId(), ex);
        }
    }
}
