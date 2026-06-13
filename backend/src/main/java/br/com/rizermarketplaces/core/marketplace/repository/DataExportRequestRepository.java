package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.DataExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, UUID> {
    List<DataExportRequest> findAllByUserIdOrderByRequestedAtDesc(UUID userId);
}
