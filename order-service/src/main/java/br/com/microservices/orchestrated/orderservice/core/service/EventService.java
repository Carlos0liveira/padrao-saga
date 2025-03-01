package br.com.microservices.orchestrated.orderservice.core.service;

import java.time.LocalDateTime;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import org.springframework.stereotype.Service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import jakarta.servlet.Filter;
import jakarta.validation.Validation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;

    public Event save(Event event) {
        return eventRepository.save(event);
    }

    public void notifyEnding(Event event) {
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(LocalDateTime.now());
        save(event);
        log.info("Order {} with saga notified! TransactionID: {}", event.getOrderId(), event.getTransactionId());
    }

    public List<Event> findAll() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    public Event findByFilters(EventFilters filters) {
        validateEmptyFilters(filters);

        if (!isEmpty(filters.getOrderId())) {
            findByOrderId(filters);
        }

        return findByTransactionId(filters);
    }

    private Event findByOrderId(EventFilters filters) {
        return eventRepository.findTop1ByOrderIdOrderByCreatedAtDesc(filters.getOrderId())
                .orElseThrow(() -> new ValidationException("Event not found by Order ID"));
    }

    private Event findByTransactionId(EventFilters filters) {
        return eventRepository.findTop1ByTransactionIdOrderByCreatedAtDesc(filters.getTransactionId())
                .orElseThrow(() -> new ValidationException("Event not found by Transaction ID"));
    }


    private void validateEmptyFilters(EventFilters filters) {
        if (isEmpty(filters.getOrderId()) && isEmpty(filters.getTransactionId())) {
            throw new ValidationException("Order ID or Transaction ID must be informed");
        }
    }

}
