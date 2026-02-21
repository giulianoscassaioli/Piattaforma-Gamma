package com.gamma.firma.service;

import com.gamma.firma.dto.AllegatoFirmatoEvent;
import com.gamma.firma.model.AllegatoFirma;
import com.gamma.firma.repository.AllegatoFirmaRepository;
import com.gamma.firma.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class FirmaService {

    private static final String TOPIC_ALLEGATO_FIRMATO = "allegato-firmato";

    @Autowired
    private AllegatoFirmaRepository allegatoFirmaRepo;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AllegatoFirma conferma(UUID allegatoId) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.get().getUserId();

        if (allegatoFirmaRepo.findByAllegatoId(allegatoId).isPresent()) {
            throw new IllegalStateException("Allegato già firmato: " + allegatoId);
        }

        AllegatoFirma firma = allegatoFirmaRepo.save(AllegatoFirma.builder()
                .allegatoId(allegatoId)
                .tenantId(tenantId)
                .userId(userId)
                .firmatoAt(LocalDateTime.now())
                .build());

        log.info("Allegato {} firmato da {} tenant {}", allegatoId, userId, tenantId);

        kafkaTemplate.send(TOPIC_ALLEGATO_FIRMATO, tenantId,
                new AllegatoFirmatoEvent(allegatoId, tenantId, userId));

        return firma;
    }
}
