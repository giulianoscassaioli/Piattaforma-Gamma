package com.gamma.pec.controller;

import com.gamma.pec.dto.AllegatoDto;
import com.gamma.pec.dto.CasellaPecRequest;
import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.repository.AllegatoRepository;
import com.gamma.pec.service.CasellaPecService;
import com.gamma.pec.tenant.TenantContext;
import com.gamma.pec.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasellaPecControllerTest {

    @Mock
    private CasellaPecService casellaPecService;

    @InjectMocks
    private CasellaPecController controller;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.builder().tenantId("tenant-1").userId("user-1").build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listaCaselleUtenteNonAdminTest() {
        Authentication auth = mockAuthentication(false);
        when(casellaPecService.listaCaselle(null, null, null, false)).thenReturn(List.of(
                new CasellaDto(UUID.randomUUID(), "mario@pec.it", List.of())
        ));

        List<CasellaDto> risultato = controller.listaCaselle(null, null, null, auth);

        assertThat(risultato).hasSize(1);
        verify(casellaPecService).listaCaselle(null, null, null, false);
    }

    @Test
    void leggiAllegatoTest() {
        UUID allegatoId = UUID.randomUUID();
        Allegato allegato = Allegato.builder()
                .id(allegatoId)
                .filename("fattura.pdf")
                .build();
        when(casellaPecService.leggiAllegato(allegatoId)).thenReturn(allegato);

        AllegatoDto risultato = controller.leggiAllegato(allegatoId);

        assertThat(risultato.filename()).isEqualTo("fattura.pdf");
        verify(casellaPecService).leggiAllegato(allegatoId);
    }

    // helper per simulare un'autenticazione utente normale
    private Authentication mockAuthentication(boolean isAdmin) {
        Authentication auth = mock(Authentication.class);
        Collection<? extends GrantedAuthority> authorities = isAdmin
                ? List.of((GrantedAuthority) () -> "ROLE_admin")
                : List.of((GrantedAuthority) () -> "ROLE_user");
        doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }
}
