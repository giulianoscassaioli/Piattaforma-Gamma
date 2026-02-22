package com.gamma.pec.controller;

import com.gamma.pec.dto.AllegatoDto;
import com.gamma.pec.dto.CasellaPecRequest;
import com.gamma.pec.dto.CasellaDto;
import com.gamma.pec.model.Allegato;
import com.gamma.pec.model.CasellaPec;
import com.gamma.pec.service.CasellaPecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/caselle-pec")
public class CasellaPecController {

    @Autowired
    private CasellaPecService casellaPecService;

    @GetMapping
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public List<CasellaDto> listaCaselle(@RequestParam(required = false) String indirizzo,
                                         @RequestParam(required = false) String mittente,
                                         @RequestParam(required = false) String oggetto,
                                         Authentication authentication) {
        return casellaPecService.listaCaselle(indirizzo, mittente, oggetto, isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public CasellaPec registraCasella(@RequestBody CasellaPecRequest request) {
        return casellaPecService.registraCasella(request.indirizzo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public void eliminaCasella(@PathVariable UUID id) {
        casellaPecService.eliminaCasella(id);
    }

    // Simula anche l'arrivo di nuovi messagg
    @GetMapping("/{id}/leggi-messaggi")
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public List<MessaggioImportatoDto> leggiMessaggi(@PathVariable UUID id,
                                                     @RequestParam(required = false) String mittente,
                                                     @RequestParam(required = false) String oggetto) {
        return casellaPecService.leggiMessaggi(id, mittente, oggetto).stream()
                .map(m -> new MessaggioImportatoDto(m.getId(), m.getMessageId(), m.getOggetto(), m.getMittente()))
                .toList();
    }

    @GetMapping("/allegati/{allegatoId}/leggi-allegato")
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public AllegatoDto leggiAllegato(@PathVariable UUID allegatoId) {
        Allegato allegato = casellaPecService.leggiAllegato(allegatoId);
        return new AllegatoDto(allegato.getId(), allegato.getFilename());
    }

    @GetMapping("/allegati/firmati")
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public List<AllegatoDto> allegatiFirmati(Authentication authentication) {
        return casellaPecService.allegatiFirmati(isAdmin(authentication)).stream()
                .map(a -> new AllegatoDto(a.getId(), a.getFilename()))
                .toList();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
    }

    public record MessaggioImportatoDto(UUID id, String messageId, String oggetto, String mittente) {}
}
