package br.com.microservices.orchestrated.paymentservice.core.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.ROLLBACK_PENDING;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final PaymentRepository paymentRepository;

    public void realizePayment(Event event) {

        try {
            checkCurrentPayment(event);
            createPendingPayment(event);

            var payment = findByOrderAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);

            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to realize payment: {}", e.getMessage());
            handleFailCurrentNotExecuted(event, e.getMessage());
        }

        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    public void realizeRefound(Event event) {
        changePaymentStatusToRefound(event);

        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on payment!");

        kafkaProducer.sendEvent(jsonUtil.toJson(event));


    }

    public void changePaymentStatusToRefound(Event event) {
        var payment =  findByOrderAndTransactionId(event);

        payment.setStatus(EPaymentStatus.REFUND);
        setEventAmountItens(event, payment);

        save(payment);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Fail trying to realize payment: " + message);
    }


    private void handleSuccess(Event event) {
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);

        addHistory(event, "Payment realized successfully!");
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

    private void changePaymentToSuccess(Payment payment) {
        payment.setStatus(EPaymentStatus.SUCCESS);
        save(payment);
    }

    private void checkCurrentPayment(Event event) {
        if (paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("Payment already exists for this order!");
        }
    }

    private void createPendingPayment(Event event) {
        var totalAmout = calculateTotalAmount(event);
        var totalItems = calculateTotalItems(event);

        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalItems(totalItems)
                .totalAmount(totalAmout)
                .build();

        this.save(payment);

        setEventAmountItens(event, payment);
    }

    private void setEventAmountItens(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(Long.valueOf(payment.getTotalItems()));
    }

    private BigDecimal calculateTotalAmount(Event event) {
        return event
                .getPayload()
                .getProducts()
                .stream()
                .map(product -> product.getProducts().getUnitValue().multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateTotalItems(Event event) {
        return Math.toIntExact(event
                .getPayload()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(0L, Long::sum));
    }

    private Payment findByOrderAndTransactionId(Event event) {
        return paymentRepository.findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by order and transaction id!"));
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    private void validateAmount(BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0.10) {
            throw new ValidationException("Amount must be greater than 0.10!");
        }
    }



}
