package com.gamma.pec.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamma.pec.dto.FirmaFallitaEvent;
import com.gamma.pec.dto.FirmaRiuscitaEvent;
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

    @KafkaListener(topics = "firma-riuscita-event", groupId = "pec-eventi")
    public void handleFirmaRiuscita(String payload) {
        FirmaRiuscitaEvent event = convertiEvento(payload, FirmaRiuscitaEvent.class);
        if (event == null) return;
        log.info("Firma riuscita ricevuta: allegato {} tenant {} user {}", event.allegatoId(), event.tenantId(), event.userId());
        casellaPecService.gestisciAllegatoFirmato(event.allegatoId(), event.tenantId(), event.userId());
    }

    @KafkaListener(topics = "firma-fallita-event", groupId = "pec-eventi")
    public void handleFirmaFallita(String payload) {
        FirmaFallitaEvent event = convertiEvento(payload, FirmaFallitaEvent.class);
        if (event == null) return;
        log.warn("Firma fallita ricevuta: allegato {} tenant {} user {}", event.allegatoId(), event.tenantId(), event.userId());
        casellaPecService.gestisciAllegatoFirmaFallita(event.allegatoId(), event.tenantId(), event.userId());
    }

    private <T> @Nullable T convertiEvento(String payload, Class<T> tipo) {
        try {
            return objectMapper.readValue(payload, tipo);
        } catch (JsonProcessingException e) {
            log.error("Messaggio Kafka non valido, ignorato: {}", payload, e);
            return null;
        }
    }
}
