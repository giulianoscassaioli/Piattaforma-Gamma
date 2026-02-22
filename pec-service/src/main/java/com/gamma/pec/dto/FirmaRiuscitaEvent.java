package com.gamma.pec.dto;

import java.util.UUID;

public record FirmaRiuscitaEvent(UUID allegatoId, String tenantId, String userId, String riuscitoAt) {}
