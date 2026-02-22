package com.gamma.pec.service;

import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.mock.AllegatoMock;
import com.gamma.pec.mock.MockConservazioneApi;
import com.gamma.pec.mock.MessaggioPecMock;
import com.gamma.pec.mock.MockPecApi;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.repository.AllegatoRepository;
import com.gamma.pec.repository.CasellaPecRepository;
import com.gamma.pec.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private MockPecApi mockPecApi;

    @Autowired
    private MockConservazioneApi mockConservazioneApi;

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

    public void eliminaCasella(UUID id) {
        CasellaPec casella = casellaPecRepo.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new NoSuchElementException("Casella non trovata: " + id));
        casellaPecRepo.delete(casella);
        log.info("Casella PEC {} eliminata per tenant {}", casella.getIndirizzo(), TenantContext.getTenantId());
    }

    @Transactional
    public List<Allegato> leggiMessaggiImportaAllegati(UUID casellaPecId, String mittente, String oggetto) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.get().getUserId();
        var casella = casellaPecRepo.findByIdAndTenantId(casellaPecId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Casella PEC non trovata: " + casellaPecId));
        List<Allegato> salvati = importaMessagiMockEAggiornaAllegati(casellaPecId, mittente, oggetto, casella, tenantId, userId);
        log.info("Casella {} tenant {}: {} allegati salvati", casella.getIndirizzo(), tenantId, salvati.size());
        return salvati;
    }

    private @NonNull List<Allegato> importaMessagiMockEAggiornaAllegati(UUID casellaPecId, String mittente, String oggetto, CasellaPec casella, String tenantId, String userId) {
        return mockPecApi.readMessages(casella.getIndirizzo()).stream()
                .filter(msg -> mittente == null || msg.getSender().equalsIgnoreCase(mittente))
                .filter(msg -> oggetto == null || msg.getSubject().toLowerCase().contains(oggetto.toLowerCase()))
                .flatMap(msg -> msg.getAllegati().stream())
                .map(att -> leggiAllegato(casellaPecId, casella, tenantId, userId, att))
                .toList();
    }

    private @NonNull Allegato leggiAllegato(UUID casellaPecId, CasellaPec casella, String tenantId, String userId, AllegatoMock att) {
        return allegatoRepo.save(Allegato.builder()
                .casellaPec(casella)
                .tenantId(tenantId)
                .userId(userId)
                .filename(tenantId + "/" + casellaPecId + "/" + att.getFilename())
                .letto(true)
                .firmato(false)
                .build());
    }

    private CasellaDto toDto(CasellaPec casella, String mittente, String oggetto) {
        List<Allegato> allegatiImportati = allegatoRepo.findByCasellaPecId(casella.getId());
        List<CasellaDto.MessaggioDto> messaggi = getMessaggi(casella, mittente, oggetto, allegatiImportati);
        return new CasellaDto(casella.getId(), casella.getIndirizzo(), messaggi);
    }

    private @NonNull List<CasellaDto.@NonNull MessaggioDto> getMessaggi(CasellaPec casella, String mittente, String oggetto, List<Allegato> allegatiImportati) {
        return mockPecApi.readMessages(casella.getIndirizzo()).stream()
                .filter(m -> mittente == null || m.getSender().equalsIgnoreCase(mittente))
                .filter(m -> oggetto == null || m.getSubject().toLowerCase().contains(oggetto.toLowerCase()))
                .map(m -> getMessaggioDto(m, allegatiImportati))
                .toList();
    }

    private static CasellaDto.@NonNull MessaggioDto getMessaggioDto(MessaggioPecMock m, List<Allegato> allegatiImportati) {
        List<CasellaDto.AllegatoDto> allegatiDto = m.getAllegati().stream()
                .map(a -> getAllegatoDto(allegatiImportati, a))
                .toList();
        return new CasellaDto.MessaggioDto(m.getId(), m.getSubject(), m.getSender(), allegatiDto);
    }

    private static CasellaDto.@NonNull AllegatoDto getAllegatoDto(List<Allegato> allegatiImportati, AllegatoMock a) {
        // controlla se già importato
        Allegato importato = allegatiImportati.stream()
                .filter(imp -> imp.getFilename().endsWith(a.getFilename()))
                .findFirst().orElse(null);
        boolean letto = importato != null;
        boolean firmato = letto && importato.isFirmato();
        UUID allegatoId = letto ? importato.getId() : null;
        return new CasellaDto.AllegatoDto(allegatoId, a.getFilename(), letto, firmato);
    }

    @Transactional
    public void gestisciAllegatoFirmato(UUID allegatoId, String tenantId, String userId) {
        allegatoRepo.findById(allegatoId).ifPresent(allegato -> {
            if (!allegato.getTenantId().equals(tenantId) || !allegato.getUserId().equals(userId)) {
                log.warn("Allegato {} non appartiene a tenant {} user {}, ignorato", allegatoId, tenantId, userId);
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
            if (allegato.isFirmato())
                allegato.setFirmato(false);
            allegatoRepo.save(allegato);
            log.warn("Rollback allegato {} marcato come firma fallita", allegato.getFilename());
        });
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
        List<CasellaPec> caselle;
        caselle = (filtroIndirizzo != null && !filtroIndirizzo.isBlank())
                ? casellaPecRepo.findByTenantIdAndIndirizzoContainingIgnoreCase(tenantId, filtroIndirizzo)
                : casellaPecRepo.findByTenantId(tenantId);
        return caselle;
    }
}
