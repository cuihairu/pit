package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedeemRecordRepo extends JpaRepository<RedeemRecordEntity, String> {

    List<RedeemRecordEntity> findByBatchIdAndPlayerKey(String batchId, String playerKey);

    long countByBatchId(String batchId);

    List<RedeemRecordEntity> findByGameIdAndPlayerKeyOrderByRedeemedAtDesc(String gameId, String playerKey);
}
