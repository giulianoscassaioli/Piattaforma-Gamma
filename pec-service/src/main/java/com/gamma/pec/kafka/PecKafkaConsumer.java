package com.gamma.pec.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamma.pec.dto.AllegatoFirmatoEvent;
import com.gamma.pec.service.CasellaPecService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PecKafkaConsumer {

    @Autowired
    private CasellaPecService casellaPecService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "allegato-firmato", groupId = "pec-eventi")
    public void handleAllegatoFirmato(String payload) {
        AllegatoFirmatoEvent event = convertiEvento(payload);
        if (event == null) return;
        log.info("Allegato firmato ricevuto: {} tenant {} user {}", event.getAllegatoId(), event.getTenantId(), event.getUserId());
        casellaPecService.gestisciAllegatoFirmato(event.getAllegatoId(), event.getTenantId(), event.getUserId());
    }

    private @Nullable AllegatoFirmatoEvent convertiEvento(String payload) {
        AllegatoFirmatoEvent event;
        try {
            event = objectMapper.readValue(payload, AllegatoFirmatoEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Messaggio Kafka non valido, ignorato: {}", payload, e);
            return null;
        }
        return event;
    }
}
