package com.gamma.firma.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allegato_firmato")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AllegatoFirmato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "allegato_id", nullable = false, unique = true)
    private UUID allegatoId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "firmato_at", nullable = false)
    private LocalDateTime firmatoAt;
}
