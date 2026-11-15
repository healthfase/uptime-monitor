package dev.uptime.monitor.repository;

import dev.uptime.monitor.entity.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckResultRepository
        extends JpaRepository<CheckResult, Long> {
}
