package com.rescuehub.repository;

import com.rescuehub.entity.VisitCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisitChargeRepository extends JpaRepository<VisitCharge, Long> {
    List<VisitCharge> findByVisitId(Long visitId);
}
