package com.gamma.firma.repository;

import com.gamma.firma.model.AllegatoFirma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AllegatoFirmaRepository extends JpaRepository<AllegatoFirma, UUID> {

    Optional<AllegatoFirma> findByAllegatoId(UUID allegatoId);
}
