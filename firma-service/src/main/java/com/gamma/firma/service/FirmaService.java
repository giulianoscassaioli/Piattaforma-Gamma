package com.gamma.firma.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamma.firma.dto.FirmaFallitaEvent;
import com.gamma.firma.dto.FirmaRiuscitaEvent;
import com.gamma.firma.model.AllegatoFirmato;
import com.gamma.firma.repository.AllegatoFirmaRepository;
import com.gamma.firma.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class FirmaService {

    private static final String TOPIC_FIRMA_RIUSCITA = "firma-riuscita-event";
    private static final String TOPIC_FIRMA_FALLITA = "firma-fallita-event";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AllegatoFirmaRepository allegatoFirmaRepo;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public AllegatoFirmato conferma(UUID allegatoId) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.get().getUserId();
        try {
            if (allegatoFirmaRepo.findByAllegatoId(allegatoId).isPresent()) {
                throw new IllegalStateException("Allegato già firmato: " + allegatoId);
            }
            AllegatoFirmato firma = salvaFirma(allegatoId, tenantId, userId);
            log.info("Allegato {} firmato da {} tenant {}", allegatoId, userId, tenantId);
            inviaEventoRiuscita(allegatoId, tenantId, userId);
            return firma;
        } catch (Exception e) {
            log.error("Firma fallita per allegato {}: {}", allegatoId, e.getMessage());
            inviaEventoFallita(allegatoId, tenantId, userId);
            throw e;
        }
    }

    private @NonNull AllegatoFirmato salvaFirma(UUID allegatoId, String tenantId, String userId) {
        return allegatoFirmaRepo.save(AllegatoFirmato.builder()
                .allegatoId(allegatoId)
                .tenantId(tenantId)
                .userId(userId)
                .firmatoAt(LocalDateTime.now())
                .build());
    }

    private void inviaEventoRiuscita(UUID allegatoId, String tenantId, String userId) {
        try {
            String payload = objectMapper.writeValueAsString(new FirmaRiuscitaEvent(allegatoId, tenantId, userId, LocalDateTime.now().toString()));
            kafkaTemplate.send(TOPIC_FIRMA_RIUSCITA, tenantId, payload);
        } catch (Exception e) {
            log.error("Errore pubblicazione evento firma-riuscita per allegato {}", allegatoId, e);
        }
    }

    private void inviaEventoFallita(UUID allegatoId, String tenantId, String userId) {
        try {
            String payload = objectMapper.writeValueAsString(new FirmaFallitaEvent(allegatoId, tenantId, userId, LocalDateTime.now().toString()));
            kafkaTemplate.send(TOPIC_FIRMA_FALLITA, tenantId, payload);
        } catch (Exception e) {
            log.error("Errore pubblicazione evento firma-fallita per allegato {}", allegatoId, e);
        }
    }
}
