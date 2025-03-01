package br.com.microservices.orchestrated.inventoryservice.core.dto;

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
