package br.com.microservices.orchestrated.paymentservice.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.microservices.orchestrated.orderservice.core.document.OrderProducts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private String id;

    private List<OrderProducts> products;

    private LocalDateTime createdAt;

    private String transactionId;

    private BigDecimal totalAmount;

    private Long totalItems;


}
