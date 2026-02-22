package com.gamma.pec.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "casella_pec")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CasellaPec {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 200)
    private String indirizzo;

    @OneToMany(mappedBy = "casellaPec", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessaggioPec> messaggi;
}
