package com.gamma.pec.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "allegato")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Allegato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casella_pec_id", nullable = false)
    private CasellaPec casellaPec;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private boolean firmato;
}
