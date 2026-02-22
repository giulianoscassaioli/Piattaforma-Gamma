package com.gamma.pec.dto;

import java.util.List;
import java.util.UUID;

public record CasellaDto(
        UUID id,
        String indirizzo,
        List<MessaggioDto> messaggi
) {
    public record MessaggioDto(UUID id, String oggetto, String mittente, List<AllegatoDto> allegati) {}
    public record AllegatoDto(UUID id, String filename, boolean letto, boolean firmato) {}
}
