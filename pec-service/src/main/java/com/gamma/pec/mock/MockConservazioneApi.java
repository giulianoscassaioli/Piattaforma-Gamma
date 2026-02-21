package com.gamma.pec.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockConservazioneApi {

    public void conservaAllegato(String tenantId, String documentName) {
        // TODO: in produzione si potrebbe fare salvataggio su storage S3
        log.info("Documento {} inviato in conservazione per tenant {}", documentName, tenantId);
    }
}
