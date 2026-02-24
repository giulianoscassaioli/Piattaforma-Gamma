package com.gamma.pec.repository;

import com.gamma.pec.model.MessaggioPec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessaggioPecRepository extends JpaRepository<MessaggioPec, UUID> {

    Optional<MessaggioPec> findByCasellaPecIdAndMessageId(UUID casellaPecId, String messageId);

    //get all paginata
}
