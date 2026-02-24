package com.gamma.pec.repository;

import com.gamma.pec.model.Allegato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AllegatoRepository extends JpaRepository<Allegato, UUID> {

    Page<Allegato> findByTenantIdAndFirmato(String tenantId, boolean firmato, Pageable pageable);

    Page<Allegato> findByTenantIdAndUserIdAndFirmato(String tenantId, String userId, boolean firmato, Pageable pageable);
}
