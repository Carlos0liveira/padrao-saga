package br.com.microservices.orchestrated.inventoryservice.core.utils;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public String toJson(Object k) {
     try {
         return objectMapper.writeValueAsString(k);
     } catch (Exception e) {
         log.error("Error converting object to json", e);
         return "";
     }
    }

    private Event toEvent(String json) {
        try {
            return objectMapper.readValue(json, Event.class);
        } catch (Exception e) {
            log.error("Error converting json to object", e);
            return null;
        }
    }

}
