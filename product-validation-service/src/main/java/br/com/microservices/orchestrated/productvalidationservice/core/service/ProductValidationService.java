package br.com.microservices.orchestrated.productvalidationservice.core.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.ROLLBACK_PENDING;
import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.SUCCESS;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProduct(Event event) {

        try {
            checkCurrentValidation(event);

            createValidation(event, Boolean.TRUE);

            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to validate products: {}", e.getMessage());
            handleFailCurrentNotExecuted(event, e.getMessage());
        }


        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    public void rollbackEvent(Event event) {
        changeValidationToFail(event);

        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on product validation!");

        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void changeValidationToFail(Event event) {
        validationRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(Boolean.FALSE);
                    validationRepository.save(validation);

                }, () -> createValidation(event, Boolean.FALSE));
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Fail To validate products: ".concat(message));

        createValidation(event, Boolean.FALSE);
    }


    private void createValidation(Event event, Boolean success) {
        var validation = Validation
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Products validated successfully");
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }


    private void checkCurrentValidation(Event event) {
        validateProductsInformed(event);

        if (Boolean.TRUE.equals(validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId()))) {
            throw new ValidationException("There's another validation in progress for this order");
        }

        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });

    }

    private void validateProductInformed(OrderProducts product) {
        if (isEmpty(product.getProduct()) || isEmpty(product.getProduct().getCode())) {
            throw new ValidationException("Product code must be informed");
        }
    }

    private void validateExistingProduct(String code) {
        if (Boolean.FALSE.equals(productRepository.existsByCode(code))) {
            throw new ValidationException("Product does not exist in database");
        }
    }

    private void validateProductsInformed(Event event) {
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("Product List is empty");
        }

        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderID and TransactionID must be informed");
        }
    }



}
