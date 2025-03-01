package br.com.microservices.orchestrated.inventoryservice.core.dto;

import java.util.List;

import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String id;

    private String transactionId;

    private String orderId;

    private Order payload;

    private String source;

    private ESagaStatus status;

    private List<History> eventHistory;
}
