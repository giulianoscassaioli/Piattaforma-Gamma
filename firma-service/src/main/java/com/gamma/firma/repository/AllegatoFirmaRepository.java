package com.gamma.firma.repository;

import com.gamma.firma.model.AllegatoFirmato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AllegatoFirmaRepository extends JpaRepository<AllegatoFirmato, UUID> {

    Optional<AllegatoFirmato> findByAllegatoId(UUID allegatoId);
}
