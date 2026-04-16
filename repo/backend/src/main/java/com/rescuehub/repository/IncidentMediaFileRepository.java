package com.rescuehub.repository;

import com.rescuehub.entity.IncidentMediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentMediaFileRepository extends JpaRepository<IncidentMediaFile, Long> {
    List<IncidentMediaFile> findByIncidentReportId(Long incidentId);
}
