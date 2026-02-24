package com.gamma.pec.service;

import com.gamma.pec.model.CasellaPec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatingCasellaPecServiceTest {

    @Mock
    private CasellaPecServiceImpl delegate;

    @InjectMocks
    private ValidatingCasellaPecService validatingService;

    @Test
    void registraCasella_indirizzoVuoto_lanciaEccezione() {
        assertThatThrownBy(() -> validatingService.registraCasella(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vuoto");
    }

    @Test
    void registraCasella_indirizzoNull_lanciaEccezione() {
        assertThatThrownBy(() -> validatingService.registraCasella(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vuoto");
    }

    @Test
    void registraCasella_senzaChiocciola_lanciaEccezione() {
        assertThatThrownBy(() -> validatingService.registraCasella("mariorossi.pec.it"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@");
    }

    @Test
    void registraCasella_indirizzoValido_delegaAlService() {
        CasellaPec casella = CasellaPec.builder().id(UUID.randomUUID())
                .tenantId("tenant-1").userId("user-1").indirizzo("mario@pec.it").build();
        when(delegate.registraCasella("mario@pec.it")).thenReturn(casella);

        CasellaPec risultato = validatingService.registraCasella("mario@pec.it");

        assertThat(risultato.getIndirizzo()).isEqualTo("mario@pec.it");
        verify(delegate).registraCasella("mario@pec.it");
    }
}
