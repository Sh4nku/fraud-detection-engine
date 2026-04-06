package com.fraud.kafka.producer;

import com.fraud.kafka.event.FraudAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertProducer {

    private static final String TOPIC = "fraud-alerts";
    private final KafkaTemplate<String, FraudAlertEvent> kafkaTemplate;

    public void publishFraudAlert(FraudAlertEvent event) {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud alert: {}", event.getFraudCaseId(), ex);
                    } else {
                        log.info("Fraud alert published for transaction: {}", event.getTransactionId());
                    }
                });
    }
}
