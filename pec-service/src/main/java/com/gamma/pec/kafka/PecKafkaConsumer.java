package com.gamma.pec.kafka;

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

    @KafkaListener(topics = "allegato-firmato", groupId = "pec-eventi")
    @Transactional
    public void handleAllegatoFirmato(AllegatoFirmatoEvent event) {
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
}
