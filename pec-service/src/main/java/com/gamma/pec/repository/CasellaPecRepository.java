package com.gamma.pec.repository;

import com.gamma.pec.model.CasellaPec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CasellaPecRepository extends JpaRepository<CasellaPec, UUID> {

    Page<CasellaPec> findByUserIdAndTenantId(String userId, String tenantId, Pageable pageable);

    Page<CasellaPec> findByTenantId(String tenantId, Pageable pageable);

    Optional<CasellaPec> findByIdAndTenantId(UUID id, String tenantId);

    Page<CasellaPec> findByUserIdAndTenantIdAndIndirizzoContainingIgnoreCase(String userId, String tenantId, String indirizzo, Pageable pageable);

    Page<CasellaPec> findByTenantIdAndIndirizzoContainingIgnoreCase(String tenantId, String indirizzo, Pageable pageable);
}
