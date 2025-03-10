package br.com.microservices.orchestrated.inventoryservice.core.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class InventoryConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.inventory-success}"
    )
    public void consumeSuccessSagaEvent(String payload) {
        log.info("Receiving success event {} from inventory-validation-success topic", payload);
        var event = jsonUtil.toEvent(payload);

        log.info(event.toString());
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.inventory-fail}"
    )
    public void consumeFailEvent(String payload) {
        log.info("Receiving rollback event {} from inventory-validation-fail topic", payload);
        var event = jsonUtil.toEvent(payload);

        log.info(event.toString());
    }

}
