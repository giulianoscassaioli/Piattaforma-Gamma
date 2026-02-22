package com.gamma.pec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirmaFallitaEvent {
    private UUID allegatoId;
    private String tenantId;
    private String userId;
    private String fallitoAt;
}
