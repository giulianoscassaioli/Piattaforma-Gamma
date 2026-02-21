package com.gamma.pec.repository;

import com.gamma.pec.model.CasellaPec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CasellaPecRepository extends JpaRepository<CasellaPec, UUID> {

    List<CasellaPec> findByUserIdAndTenantId(String userId, String tenantId);

    List<CasellaPec> findByTenantId(String tenantId);

    Optional<CasellaPec> findByIdAndTenantId(UUID id, String tenantId);

    List<CasellaPec> findByUserIdAndTenantIdAndIndirizzoContainingIgnoreCase(String userId, String tenantId, String indirizzo);

    List<CasellaPec> findByTenantIdAndIndirizzoContainingIgnoreCase(String tenantId, String indirizzo);
}
