package com.gamma.pec.dto;

import java.util.UUID;

public record FirmaFallitaEvent(UUID allegatoId, String tenantId, String userId, String fallitoAt) {}
