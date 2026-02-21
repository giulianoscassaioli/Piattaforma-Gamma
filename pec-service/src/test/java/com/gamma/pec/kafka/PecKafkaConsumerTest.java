package com.gamma.pec.kafka;

import com.gamma.pec.dto.AllegatoFirmatoEvent;
import com.gamma.pec.mock.MockConservazioneApi;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.repository.AllegatoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecKafkaConsumerTest {

    @Mock
    private AllegatoRepository allegatoRepo;

    @Mock
    private MockConservazioneApi mockConservazioneApi;

    @InjectMocks
    private PecKafkaConsumer pecKafkaConsumer;

    @Test
    void handleAllegatoFirmato_marcaFirmatoEConserva() {
        UUID allegatoId = UUID.randomUUID();
        Allegato allegato = Allegato.builder()
                .id(allegatoId)
                .tenantId("tenant-1")
                .userId("user-1")
                .filename("fattura.pdf")
                .firmato(false)
                .build();

        when(allegatoRepo.findById(allegatoId)).thenReturn(Optional.of(allegato));
        when(allegatoRepo.save(any())).thenReturn(allegato);

        pecKafkaConsumer.handleAllegatoFirmato(new AllegatoFirmatoEvent(allegatoId, "tenant-1", "user-1"));

        assertThat(allegato.isFirmato()).isTrue();
        verify(allegatoRepo).save(allegato);
        verify(mockConservazioneApi).conservaAllegato("tenant-1", "fattura.pdf");
    }

    @Test
    void handleAllegatoFirmato_ignoraSeTenantsNonCorrispondono() {
        UUID allegatoId = UUID.randomUUID();
        Allegato allegato = Allegato.builder()
                .id(allegatoId)
                .tenantId("tenant-1")
                .userId("user-1")
                .filename("fattura.pdf")
                .firmato(false)
                .build();

        when(allegatoRepo.findById(allegatoId)).thenReturn(Optional.of(allegato));

        // evento con tenant diverso
        pecKafkaConsumer.handleAllegatoFirmato(new AllegatoFirmatoEvent(allegatoId, "tenant-ALTRO", "user-1"));

        assertThat(allegato.isFirmato()).isFalse();
        verify(allegatoRepo, never()).save(any());
        verify(mockConservazioneApi, never()).conservaAllegato(any(), any());
    }
}
