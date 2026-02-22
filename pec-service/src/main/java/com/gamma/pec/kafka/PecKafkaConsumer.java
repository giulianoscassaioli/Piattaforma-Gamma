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
        FirmaRiuscitaEvent event = convertiEventoRiuscita(payload);
        if (event == null) return;
        log.info("Firma riuscita ricevuta: allegato {} tenant {} user {}", event.getAllegatoId(), event.getTenantId(), event.getUserId());
        casellaPecService.gestisciAllegatoFirmato(event.getAllegatoId(), event.getTenantId(), event.getUserId());
    }

    @KafkaListener(topics = "firma-fallita-event", groupId = "pec-eventi")
    public void handleFirmaFallita(String payload) {
        FirmaFallitaEvent event = convertiEventoFallita(payload);
        if (event == null) return;
        log.warn("Firma fallita ricevuta: allegato {} tenant {} user {}", event.getAllegatoId(), event.getTenantId(), event.getUserId());
        casellaPecService.gestisciAllegatoFirmaFallita(event.getAllegatoId(), event.getTenantId(), event.getUserId());
    }

    private @Nullable FirmaRiuscitaEvent convertiEventoRiuscita(String payload) {
        try {
            return objectMapper.readValue(payload, FirmaRiuscitaEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Messaggio firma-riuscita-event non valido, ignorato: {}", payload, e);
            return null;
        }
    }

    private @Nullable FirmaFallitaEvent convertiEventoFallita(String payload) {
        try {
            return objectMapper.readValue(payload, FirmaFallitaEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Messaggio firma-fallita-event non valido, ignorato: {}", payload, e);
            return null;
        }
    }
}
