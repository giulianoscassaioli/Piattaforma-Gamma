package com.gamma.pec.kafka;

import com.gamma.pec.service.CasellaPecService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecKafkaConsumerTest {

    @Mock
    private CasellaPecService casellaPecService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private PecKafkaConsumer pecKafkaConsumer;

    @Test
    void handleFirmaRiuscita_delegaAlServizio() {
        UUID allegatoId = UUID.randomUUID();
        String payload = """
                {"allegatoId":"%s","tenantId":"tenant-1","userId":"user-1","riuscitoAt":"2026-02-22T09:00:00"}
                """.formatted(allegatoId);

        pecKafkaConsumer.handleFirmaRiuscita(payload);

        verify(casellaPecService).gestisciAllegatoFirmato(allegatoId, "tenant-1", "user-1");
        verify(casellaPecService, never()).gestisciAllegatoFirmaFallita(any(), any(), any());
    }

    @Test
    void handleFirmaFallita_delegaAlServizio() {
        UUID allegatoId = UUID.randomUUID();
        String payload = """
                {"allegatoId":"%s","tenantId":"tenant-1","userId":"user-1","fallitoAt":"2026-02-22T09:00:00"}
                """.formatted(allegatoId);

        pecKafkaConsumer.handleFirmaFallita(payload);

        verify(casellaPecService).gestisciAllegatoFirmaFallita(allegatoId, "tenant-1", "user-1");
        verify(casellaPecService, never()).gestisciAllegatoFirmato(any(), any(), any());
    }

    @Test
    void handleFirmaRiuscita_payloadNonValido_nonChiamaNulla() {
        pecKafkaConsumer.handleFirmaRiuscita("non-json");

        verify(casellaPecService, never()).gestisciAllegatoFirmato(any(), any(), any());
    }

    @Test
    void handleFirmaFallita_payloadNonValido_nonChiamaNulla() {
        pecKafkaConsumer.handleFirmaFallita("non-json");

        verify(casellaPecService, never()).gestisciAllegatoFirmaFallita(any(), any(), any());
    }
}
