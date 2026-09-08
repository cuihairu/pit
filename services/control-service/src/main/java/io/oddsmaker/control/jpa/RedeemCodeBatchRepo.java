package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedeemCodeBatchRepo extends JpaRepository<RedeemCodeBatchEntity, String> {

    List<RedeemCodeBatchEntity> findByGameIdAndDeletedAtIsNullOrderByCreatedAtDesc(String gameId);

    Optional<RedeemCodeBatchEntity> findByIdAndDeletedAtIsNull(String id);
}
