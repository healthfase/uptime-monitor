
package dev.uptime.monitor.repository;

import dev.uptime.monitor.entity.CheckResult;
import dev.uptime.monitor.entity.Site;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface CheckResultRepository
        extends JpaRepository<CheckResult, Long> {

    Optional<CheckResult> findTopBySiteOrderByCheckedAtDesc(Site site);

    List<CheckResult> findBySiteOrderByCheckedAtDesc(
            Site site,
            Pageable pageable
    );

    long countBySite(Site site);

    long countBySiteAndOnlineTrue(Site site);

    long countBySiteAndOnlineFalse(Site site);

    @Query("""
        SELECT AVG(c.responseTimeMs)
        FROM CheckResult c
        WHERE c.site = :site
        """)
    Double findAverageResponseTimeBySite(Site site);
}
