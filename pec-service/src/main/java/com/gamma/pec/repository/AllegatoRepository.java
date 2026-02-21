package com.gamma.pec.repository;

import com.gamma.pec.model.Allegato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllegatoRepository extends JpaRepository<Allegato, UUID> {

    List<Allegato> findByCasellaPecId(UUID casellaPecId);

    List<Allegato> findByTenantIdAndFirmato(String tenantId, boolean firmato);

    List<Allegato> findByTenantIdAndUserIdAndFirmato(String tenantId, String userId, boolean firmato);
}
