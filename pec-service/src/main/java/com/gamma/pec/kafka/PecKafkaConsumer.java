package com.gamma.pec.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamma.pec.dto.AllegatoFirmatoEvent;
import com.gamma.pec.mock.MockConservazioneApi;
import com.gamma.pec.repository.AllegatoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PecKafkaConsumer {

    @Autowired
    private AllegatoRepository allegatoRepo;

    @Autowired
    private MockConservazioneApi mockConservazioneApi;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "allegato-firmato", groupId = "pec-eventi")
    @Transactional
    public void handleAllegatoFirmato(String payload) {
        AllegatoFirmatoEvent event = convertiPayload(payload);
        log.info("Allegato firmato ricevuto: {} tenant {} user {}", event.getAllegatoId(), event.getTenantId(), event.getUserId());
        try {
            allegatoRepo.findById(event.getAllegatoId()).ifPresent(allegato -> {
                if (!allegato.getTenantId().equals(event.getTenantId()) ||
                        !allegato.getUserId().equals(event.getUserId())) {
                    log.warn("Allegato {} non appartiene a tenant {} user {}, ignorato",
                            event.getAllegatoId(), event.getTenantId(), event.getUserId());
                    return;
                }
                allegato.setFirmato(true);
                allegatoRepo.save(allegato);
                mockConservazioneApi.conservaAllegato(event.getTenantId(), allegato.getFilename());
                log.info("Allegato {} inviato in conservazione", allegato.getFilename());
            });
        } catch (Exception e) {
            log.error("Errore conservazione allegato {}", event.getAllegatoId(), e);
        }
    }

    private AllegatoFirmatoEvent convertiPayload(String payload) {
        AllegatoFirmatoEvent event;
        try {
            event = objectMapper.readValue(payload, AllegatoFirmatoEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Errore in convertire evento", e);
            throw new RuntimeException(e);
        }
        return event;
    }
}
