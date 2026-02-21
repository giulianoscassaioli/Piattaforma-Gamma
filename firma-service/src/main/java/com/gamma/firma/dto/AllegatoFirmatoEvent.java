package com.gamma.firma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllegatoFirmatoEvent {
    private UUID allegatoId;
    private String tenantId;
    private String userId;
}
