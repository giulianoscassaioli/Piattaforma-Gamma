package com.gamma.pec.service;

import com.gamma.pec.mock.AllegatoMock;
import com.gamma.pec.mock.MessaggioPecMock;
import com.gamma.pec.mock.MockPecApi;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.model.MessaggioPec;
import com.gamma.pec.repository.AllegatoRepository;
import com.gamma.pec.repository.CasellaPecRepository;
import com.gamma.pec.repository.MessaggioPecRepository;
import com.gamma.pec.tenant.TenantContext;
import com.gamma.pec.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
    private MessaggioPecRepository messaggioPecRepo;

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
    void registraCasellaTest() {
        CasellaPec casella = CasellaPec.builder().id(UUID.randomUUID())
                .tenantId("tenant-1").userId("user-1").indirizzo("mario@pec.it").build();
        when(casellaPecRepo.save(any())).thenReturn(casella);

        CasellaPec risultato = casellaPecService.registraCasella("mario@pec.it");

        assertThat(risultato.getIndirizzo()).isEqualTo("mario@pec.it");
        verify(casellaPecRepo).save(any());
    }

    @Test
    void leggiMessaggiTest() {
        UUID casellaPecId = UUID.randomUUID();
        CasellaPec casella = CasellaPec.builder().id(casellaPecId)
                .tenantId("tenant-1").indirizzo("mario@pec.it").build();

        when(casellaPecRepo.findByIdAndTenantId(casellaPecId, "tenant-1")).thenReturn(Optional.of(casella));
        when(mockPecApi.leggiIncomingMessages("mario@pec.it")).thenReturn(List.of(
                MessaggioPecMock.builder().id("msg-001").oggetto("Fattura Gennaio 2026").mittente("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_gennaio.pdf").build())).build(),
                MessaggioPecMock.builder().id("msg-002").oggetto("Fattura Febbraio 2026").mittente("fornitore@pec.it")
                        .allegati(List.of(AllegatoMock.builder().filename("fattura_febbraio.pdf").build())).build()
        ));
        MessaggioPec messaggio = MessaggioPec.builder().id(UUID.randomUUID()).casellaPec(casella)
                .messageId("msg-001").oggetto("Fattura Gennaio 2026").mittente("fornitore@pec.it")
                .allegati(new ArrayList<>()).build();
        when(messaggioPecRepo.findByCasellaPecIdAndMessageId(casellaPecId, "msg-001")).thenReturn(Optional.empty());
        when(messaggioPecRepo.save(any())).thenReturn(messaggio);

        List<MessaggioPec> risultato = casellaPecService.leggiMessaggi(casellaPecId, null, "Gennaio");

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getOggetto()).contains("Gennaio");
    }
}
