package io.oddsmaker.control.experiment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentMetricSnapshotRepo extends JpaRepository<ExperimentMetricSnapshotEntity, String> {

    List<ExperimentMetricSnapshotEntity> findByExperimentIdOrderByWindowStartAsc(String experimentId);

    Optional<ExperimentMetricSnapshotEntity> findByExperimentIdAndMetricNameAndVariantAndWindowStart(
        String experimentId, String metricName, String variant, long windowStart);
}
