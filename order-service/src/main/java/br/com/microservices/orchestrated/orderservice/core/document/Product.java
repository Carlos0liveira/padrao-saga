package br.com.microservices.orchestrated.orderservice.core.document;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private String code;
    private BigDecimal unitValue;
}
