package com.gamma.pec.service;

import com.gamma.pec.mock.AllegatoMock;
import com.gamma.pec.mock.MessaggioPecMock;
import com.gamma.pec.mock.MockPecApi;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.repository.AllegatoRepository;
import com.gamma.pec.repository.CasellaPecRepository;
import com.gamma.pec.tenant.TenantContext;
import com.gamma.pec.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasellaPecServiceTest {

    @Mock
    private CasellaPecRepository casellaPecRepo;

    @Mock
    private AllegatoRepository allegatoRepo;

    @Mock
    private MockPecApi mockPecApi;

    @InjectMocks
    private CasellaPecService casellaPecService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.builder().tenantId("tenant-1").userId("user-1").build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registraCasella_salvaERitornaLaCasella() {
        CasellaPec casella = CasellaPec.builder().id(UUID.randomUUID())
                .tenantId("tenant-1").userId("user-1").indirizzo("mario@pec.it").build();

        when(casellaPecRepo.save(any())).thenReturn(casella);

        CasellaPec risultato = casellaPecService.registraCasella("mario@pec.it");

        assertThat(risultato.getIndirizzo()).isEqualTo("mario@pec.it");
        verify(casellaPecRepo).save(any());
    }

    @Test
    void leggiImportaAllegati_filtraPerOggetto() {
        UUID casellaPecId = UUID.randomUUID();
        CasellaPec casella = CasellaPec.builder().id(casellaPecId)
                .tenantId("tenant-1").indirizzo("mario@pec.it").build();

        when(casellaPecRepo.findByIdAndTenantId(casellaPecId, "tenant-1")).thenReturn(Optional.of(casella));
        when(mockPecApi.readMessages("mario@pec.it")).thenReturn(List.of(
                MessaggioPecMock.builder().id("msg-001").subject("Fattura Gennaio 2026").sender("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_gennaio.pdf").build())).build(),
                MessaggioPecMock.builder().id("msg-002").subject("Fattura Febbraio 2026").sender("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_febbraio.pdf").build())).build()
        ));
        when(allegatoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Allegato> risultato = casellaPecService.leggiImportaAllegati(casellaPecId, null, "Gennaio");

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getFilename()).contains("fattura_gennaio.pdf");
    }

    @Test
    void leggiImportaAllegati_lanceEccezioneSeNonEsiste() {
        UUID casellaPecId = UUID.randomUUID();
        when(casellaPecRepo.findByIdAndTenantId(casellaPecId, "tenant-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casellaPecService.leggiImportaAllegati(casellaPecId, null, null))
                .isInstanceOf(NoSuchElementException.class);
    }
}
