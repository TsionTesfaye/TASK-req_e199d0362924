package com.rescuehub.repository;

import com.rescuehub.entity.SampledVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SampledVisitRepository extends JpaRepository<SampledVisit, Long> {
    List<SampledVisit> findBySamplingRunId(Long runId);
    boolean existsByVisitId(Long visitId);
}
