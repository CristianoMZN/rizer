package br.com.rizermarketplaces.core.marketplace.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    @Query("SELECT a FROM AuditEntry a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<AuditEntry> findByTenant(UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditEntry a WHERE a.actorUserId = :actorId ORDER BY a.createdAt DESC")
    Page<AuditEntry> findByActor(UUID actorId, Pageable pageable);

    @Query("SELECT a FROM AuditEntry a WHERE a.action = :action ORDER BY a.createdAt DESC")
    List<AuditEntry> findByAction(String action, Pageable pageable);
}
