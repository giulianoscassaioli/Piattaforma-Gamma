package com.gamma.firma.service;

import com.gamma.firma.model.AllegatoFirmato;
import com.gamma.firma.repository.AllegatoFirmaRepository;
import com.gamma.firma.tenant.TenantContext;
import com.gamma.firma.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirmaServiceTest {

    @Mock
    private AllegatoFirmaRepository allegatoFirmaRepo;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private FirmaService firmaService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.builder()
                .tenantId("tenant-1")
                .userId("user-1")
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void conferma_creaAllegatoFirmaEPubblicaEvento() {
        UUID allegatoId = UUID.randomUUID();
        AllegatoFirmato firma = AllegatoFirmato.builder()
                .id(UUID.randomUUID())
                .allegatoId(allegatoId)
                .tenantId("tenant-1")
                .userId("user-1")
                .firmatoAt(LocalDateTime.now())
                .build();

        when(allegatoFirmaRepo.findByAllegatoId(allegatoId)).thenReturn(Optional.empty());
        when(allegatoFirmaRepo.save(any())).thenReturn(firma);

        AllegatoFirmato risultato = firmaService.conferma(allegatoId);

        assertThat(risultato.getAllegatoId()).isEqualTo(allegatoId);
        assertThat(risultato.getTenantId()).isEqualTo("tenant-1");
        verify(allegatoFirmaRepo).save(any());
        verify(kafkaTemplate).send(eq("allegato-firmato"), eq("tenant-1"), any());
    }

    @Test
    void conferma_lanceEccezioneSeAllegatoGiaFirmato() {
        UUID allegatoId = UUID.randomUUID();
        when(allegatoFirmaRepo.findByAllegatoId(allegatoId))
                .thenReturn(Optional.of(AllegatoFirmato.builder().build()));

        assertThatThrownBy(() -> firmaService.conferma(allegatoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("già firmato");
    }
}
