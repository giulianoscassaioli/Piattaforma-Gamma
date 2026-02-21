package com.gamma.pec.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MockPecApi {

    public List<MessaggioPecMock> readMessages(String mailbox) {
        log.info("Mock servizio Lettura messaggi da {}", mailbox);

        // mocks
        return List.of(
                MessaggioPecMock.builder()
                        .id("msg-001")
                        .subject("Fattura Gennaio 2026")
                        .sender("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_gennaio.pdf").build()))
                        .build(),
                MessaggioPecMock.builder()
                        .id("msg-002")
                        .subject("Fattura Febbraio 2026")
                        .sender("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_febbraio.pdf").build()))
                        .build()
//                MessaggioPecMock.builder()
//                        .id("msg-003")
//                        .subject("Nota di credito Gennaio 2026")
//                        .sender("fornitore@pec.it")
//                        .allegati(List.of(AllegatoMock.builder().filename("nota_credito.pdf").build()))
//                        .build()
        );
    }
}
