package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailRepo extends JpaRepository<MailEntity, String> {

    List<MailEntity> findByGameIdAndDeletedAtIsNullOrderByCreatedAtDesc(String gameId);

    Optional<MailEntity> findByIdAndDeletedAtIsNull(String id);

    List<MailEntity> findByStatusAndExpireAtLessThanEqualAndDeletedAtIsNull(
        MailEntity.Status status, LocalDateTime now);

    /** 玩家收件箱：已发送、未过期、全环境或指定环境、全服或包含该玩家 */
    @Query("SELECT m FROM MailEntity m WHERE m.gameId = :gameId AND m.status = 'SENT' "
        + "AND m.deletedAt IS NULL "
        + "AND (m.expireAt IS NULL OR m.expireAt > :now) "
        + "AND (m.environmentId IS NULL OR m.environmentId = :environmentId) "
        + "AND (m.scope = 'ALL' OR m.recipients LIKE %:playerKey%) "
        + "ORDER BY m.sentAt DESC")
    List<MailEntity> findInbox(@Param("gameId") String gameId,
                               @Param("environmentId") String environmentId,
                               @Param("playerKey") String playerKey,
                               @Param("now") LocalDateTime now);
}
