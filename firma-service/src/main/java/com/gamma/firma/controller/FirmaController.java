package com.gamma.firma.controller;

import com.gamma.firma.dto.ConfermaResponse;
import com.gamma.firma.service.FirmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/firma")
public class FirmaController {

    @Autowired
    private FirmaService firmaService;

    @PostMapping("/{allegatoId}/conferma")
    @PreAuthorize("hasAnyRole('user', 'admin')")
    public ConfermaResponse conferma(@PathVariable UUID allegatoId) {
        var firma = firmaService.conferma(allegatoId);
        return new ConfermaResponse(firma.getAllegatoId(), firma.getFirmatoAt());
    }
}
