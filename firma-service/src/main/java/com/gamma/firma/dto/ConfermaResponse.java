package com.gamma.firma.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfermaResponse(UUID allegatoId, LocalDateTime firmatoAt) {}
