package com.gamma.pec.service;

import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.model.MessaggioPec;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface CasellaPecService {

    List<MessaggioPec> leggiMessaggi(UUID casellaPecId, String mittente, String oggetto);

    void gestisciAllegatoFirmato(UUID allegatoId, String tenantId, String userId);

    void gestisciAllegatoFirmaFallita(UUID allegatoId, String tenantId, String userId);

    void eliminaCasella(UUID id);

    Page<CasellaDto> listaCaselle(String filtroIndirizzo, String mittente, String oggetto,
                                   boolean isAdmin, int pagina, int dimensione);

    Allegato leggiAllegato(UUID allegatoId);

    CasellaPec registraCasella(String indirizzo);

    Page<Allegato> allegatiFirmati(boolean isAdmin, int pagina, int dimensione);
}
