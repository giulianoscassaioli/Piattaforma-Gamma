package com.gamma.pec.service;

import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.mock.AllegatoMock;
import com.gamma.pec.mock.MockConservazioneApi;
import com.gamma.pec.mock.MessaggioPecMock;
import com.gamma.pec.mock.MockPecApi;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.model.MessaggioPec;
import com.gamma.pec.repository.AllegatoRepository;
import com.gamma.pec.repository.CasellaPecRepository;
import com.gamma.pec.repository.MessaggioPecRepository;
import com.gamma.pec.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class CasellaPecService {

    @Autowired
    private CasellaPecRepository casellaPecRepo;

    @Autowired
    private AllegatoRepository allegatoRepo;

    @Autowired
    private MessaggioPecRepository messaggioPecRepo;

    @Autowired
    private MockPecApi mockPecApi;

    @Autowired
    private MockConservazioneApi mockConservazioneApi;

    @Transactional
    public List<MessaggioPec> leggiMessaggi(UUID casellaPecId, String mittente, String oggetto) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.get().getUserId();
        CasellaPec casella = casellaPecRepo.findByIdAndTenantId(casellaPecId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Casella PEC non trovata: " + casellaPecId));
        List<MessaggioPec> messaggi = new ArrayList<>();
        for (MessaggioPecMock msgMock : mockPecApi.leggiIncomingMessages(casella.getIndirizzo())) {
            if (daSkippare(mittente, oggetto, msgMock)) continue;
            MessaggioPec messaggio = getOSalvaMessaggio(casellaPecId, msgMock, casella);
            for (AllegatoMock allegatoMock : msgMock.getAllegati()) {
                boolean esisteGia = messaggio.getAllegati().stream()
                        .anyMatch(a -> a.getFilename().endsWith(allegatoMock.getFilename()));
                if (!esisteGia) {
                    salvaAllegato(casellaPecId, allegatoMock, messaggio, tenantId, userId);
                }
            }
            messaggi.add(messaggio);
        }
        log.info("Casella {} tenant {}: {} messaggi letti", casella.getIndirizzo(), tenantId, messaggi.size());
        return messaggi;
    }

    @Transactional
    public void gestisciAllegatoFirmato(UUID allegatoId, String tenantId, String userId) {
        allegatoRepo.findById(allegatoId).ifPresent(allegato -> {
            if (!allegato.getTenantId().equals(tenantId) || !allegato.getUserId().equals(userId)) {
                log.warn("Allegato {} non appartiene a tenant {} user {}, ignorato", allegatoId, tenantId, userId);
                return;
            }
            if (!allegato.isLetto()) {
                // TODO si dovrebbe compensare anche nel altro micrsoervizio
                log.warn("Allegato {} non ancora letto, firma ignorata", allegatoId);
                return;
            }
            allegato.setFirmato(true);
            allegatoRepo.save(allegato);
            mockConservazioneApi.conservaAllegato(tenantId, allegato.getFilename());
            log.info("Allegato {} inviato in conservazione", allegato.getFilename());
        });
    }

    @Transactional
    public void gestisciAllegatoFirmaFallita(UUID allegatoId, String tenantId, String userId) {
        allegatoRepo.findById(allegatoId).ifPresent(allegato -> {
            if (!allegato.getTenantId().equals(tenantId) || !allegato.getUserId().equals(userId)) {
                log.warn("Allegato {} non appartiene a tenant {} user {}, ignorato", allegatoId, tenantId, userId);
                return;
            }
            if (allegato.isFirmato()) {
                allegato.setFirmato(false);
                allegatoRepo.save(allegato);
                log.warn("Rollback allegato {} marcato come firma fallita", allegato.getFilename());
            }
        });
    }

    public void eliminaCasella(UUID id) {
        CasellaPec casella = casellaPecRepo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new NoSuchElementException("Casella non trovata: " + id));
        casellaPecRepo.delete(casella);
        log.info("Casella PEC {} eliminata per tenant {}", casella.getIndirizzo(), TenantContext.getTenantId());
    }

    public List<CasellaDto> listaCaselle(String filtroIndirizzo, String mittente, String oggetto, boolean isAdmin) {
        String userId = TenantContext.get().getUserId();
        String tenantId = TenantContext.getTenantId();
        List<CasellaPec> caselle;
        if (isAdmin) {
            caselle = leggiCaselleDiTuttoIlTenant(filtroIndirizzo, tenantId);
        } else {
            caselle = leggiCaselleUtente(filtroIndirizzo, userId, tenantId);
        }
        return caselle.stream().map(c -> toDto(c, mittente, oggetto)).toList();
    }

    @Transactional
    public Allegato leggiAllegato(UUID allegatoId) {
        String tenantId = TenantContext.getTenantId();
        Allegato allegato = allegatoRepo.findById(allegatoId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Allegato non trovato: " + allegatoId));
        if (!allegato.isLetto()) {
            allegato.setLetto(true);
            allegatoRepo.save(allegato);
        }
        log.info("Allegato {} letto", allegatoId);
        return allegato;
    }

    public CasellaPec registraCasella(String indirizzo) {
        CasellaPec casella = CasellaPec.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(TenantContext.get().getUserId())
                .indirizzo(indirizzo)
                .build();
        log.info("Casella PEC {} registrata per utente {} tenant {}",
                indirizzo, TenantContext.get().getUserId(), TenantContext.getTenantId());
        return casellaPecRepo.save(casella);
    }

    private static boolean daSkippare(String mittente, String oggetto, MessaggioPecMock msgMock) {
        if (mittente != null && !msgMock.getMittente().equalsIgnoreCase(mittente)) return true;
        return oggetto != null && !msgMock.getOggetto().toLowerCase().contains(oggetto.toLowerCase());
    }

    private @NonNull MessaggioPec getOSalvaMessaggio(UUID casellaPecId, MessaggioPecMock msgMock, CasellaPec casella) {
        return messaggioPecRepo.findByCasellaPecIdAndMessageId(casellaPecId, msgMock.getId())
                .orElseGet(() -> messaggioPecRepo.save(MessaggioPec.builder()
                        .casellaPec(casella)
                        .messageId(msgMock.getId())
                        .oggetto(msgMock.getOggetto())
                        .mittente(msgMock.getMittente())
                        .allegati(new ArrayList<>())
                        .build()));
    }

    private void salvaAllegato(UUID casellaPecId, AllegatoMock allegatoMock, MessaggioPec messaggio, String tenantId, String userId) {
        allegatoRepo.save(Allegato.builder()
                .messaggio(messaggio)
                .tenantId(tenantId)
                .userId(userId)
                .filename(tenantId + "/" + casellaPecId + "/" + allegatoMock.getFilename())
                .letto(false)
                .firmato(false)
                .build());
    }

    private CasellaDto toDto(CasellaPec casella, String mittente, String oggetto) {
        List<CasellaDto.MessaggioDto> messaggiDto = casella.getMessaggi().stream()
                .filter(m -> mittente == null || m.getMittente().equalsIgnoreCase(mittente))
                .filter(m -> oggetto == null || m.getOggetto().toLowerCase().contains(oggetto.toLowerCase()))
                .map(m -> {
                    List<CasellaDto.AllegatoDto> allegatiDto = m.getAllegati().stream()
                            .map(a -> new CasellaDto.AllegatoDto(a.getId(), a.getFilename(), a.isLetto(), a.isFirmato()))
                            .toList();
                    return new CasellaDto.MessaggioDto(m.getId(), m.getOggetto(), m.getMittente(), allegatiDto);
                })
                .toList();
        return new CasellaDto(casella.getId(), casella.getIndirizzo(), messaggiDto);
    }

    public List<Allegato> allegatiFirmati(boolean isAdmin) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.get().getUserId();
        return isAdmin
                ? allegatoRepo.findByTenantIdAndFirmato(tenantId, true)
                : allegatoRepo.findByTenantIdAndUserIdAndFirmato(tenantId, userId, true);
    }

    private List<CasellaPec> leggiCaselleUtente(String filtroIndirizzo, String userId, String tenantId) {
        return (filtroIndirizzo != null && !filtroIndirizzo.isBlank())
                ? casellaPecRepo.findByUserIdAndTenantIdAndIndirizzoContainingIgnoreCase(userId, tenantId, filtroIndirizzo)
                : casellaPecRepo.findByUserIdAndTenantId(userId, tenantId);
    }

    private List<CasellaPec> leggiCaselleDiTuttoIlTenant(String filtroIndirizzo, String tenantId) {
        return (filtroIndirizzo != null && !filtroIndirizzo.isBlank())
                ? casellaPecRepo.findByTenantIdAndIndirizzoContainingIgnoreCase(tenantId, filtroIndirizzo)
                : casellaPecRepo.findByTenantId(tenantId);
    }
}
