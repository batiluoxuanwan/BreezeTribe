package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Report;

public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
}
