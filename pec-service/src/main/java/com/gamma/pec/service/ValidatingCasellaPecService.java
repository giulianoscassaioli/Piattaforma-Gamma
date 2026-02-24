package com.gamma.pec.service;

import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.model.MessaggioPec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@Slf4j
public class ValidatingCasellaPecService implements CasellaPecService {

    @Autowired
    @Qualifier("casellaPecServiceImpl")
    private CasellaPecService delegate;

    @Override
    public CasellaPec registraCasella(String indirizzo) {
        if (indirizzo == null || indirizzo.isBlank()) {
            throw new IllegalArgumentException("Indirizzo PEC non valido: non può essere vuoto");
        }
        if (!indirizzo.contains("@")) {
            throw new IllegalArgumentException("Indirizzo PEC non valido: deve contenere @");
        }
        log.debug("Validazione indirizzo PEC superata: {}", indirizzo);
        return delegate.registraCasella(indirizzo);
    }

    @Override
    public List<MessaggioPec> leggiMessaggi(UUID casellaPecId, String mittente, String oggetto) {
        return delegate.leggiMessaggi(casellaPecId, mittente, oggetto);
    }

    @Override
    public void gestisciAllegatoFirmato(UUID allegatoId, String tenantId, String userId) {
        delegate.gestisciAllegatoFirmato(allegatoId, tenantId, userId);
    }

    @Override
    public void gestisciAllegatoFirmaFallita(UUID allegatoId, String tenantId, String userId) {
        delegate.gestisciAllegatoFirmaFallita(allegatoId, tenantId, userId);
    }

    @Override
    public void eliminaCasella(UUID id) {
        delegate.eliminaCasella(id);
    }

    @Override
    public Page<CasellaDto> listaCaselle(String filtroIndirizzo, String mittente, String oggetto,
                                          boolean isAdmin, int pagina, int dimensione) {
        return delegate.listaCaselle(filtroIndirizzo, mittente, oggetto, isAdmin, pagina, dimensione);
    }

    @Override
    public Allegato leggiAllegato(UUID allegatoId) {
        return delegate.leggiAllegato(allegatoId);
    }

    @Override
    public Page<Allegato> allegatiFirmati(boolean isAdmin, int pagina, int dimensione) {
        return delegate.allegatiFirmati(isAdmin, pagina, dimensione);
    }
}
