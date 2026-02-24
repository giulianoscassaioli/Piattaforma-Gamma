package com.gamma.pec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messaggio_pec")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessaggioPec {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casella_pec_id", nullable = false)
    private CasellaPec casellaPec;

    @Column(name = "message_id")
    private String messageId;

    @Column
    private String oggetto;

    @Column
    private String mittente;

    @OneToMany(mappedBy = "messaggio", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<Allegato> allegati;
}
