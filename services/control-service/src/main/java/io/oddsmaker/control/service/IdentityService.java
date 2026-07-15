package io.oddsmaker.control.service;

import io.oddsmaker.control.jpa.IdentityEntity;
import io.oddsmaker.control.jpa.IdentityLinkEntity;
import io.oddsmaker.control.jpa.IdentityLinkRepo;
import io.oddsmaker.control.jpa.IdentityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 身份查询服务。
 * 封装 IdentityRepo / IdentityLinkRepo 的只读查询，供 IdentityController 使用。
 * 身份写入由 IdentityConsumer（消费 Kafka oddsmaker.identity_events）负责，本服务只读。
 */
@Service
@Transactional(readOnly = true)
public class IdentityService {

    @Autowired
    private IdentityRepo identityRepo;

    @Autowired
    private IdentityLinkRepo identityLinkRepo;

    /** 按 id 取主身份（校验 gameId 匹配 + 未删除） */
    public Optional<IdentityEntity> getIdentity(String gameId, String identityId) {
        return identityRepo.findById(identityId)
                .filter(i -> gameId == null || gameId.equals(i.gameId))
                .filter(i -> i.deletedAt == null);
    }

    public Optional<IdentityEntity> findByDevice(String gameId, String deviceId) {
        return identityRepo.findByDeviceId(gameId, deviceId);
    }

    public List<IdentityEntity> findByUser(String gameId, String userId) {
        return identityRepo.findByUserId(gameId, userId);
    }

    public Optional<IdentityEntity> findByPlayer(String gameId, String playerId) {
        return identityRepo.findByPlayerId(gameId, playerId);
    }

    public List<IdentityLinkEntity> getLinks(String identityId) {
        return identityLinkRepo.findByIdentityId(identityId);
    }

    /**
     * 已知某标识符（device_id / player_id / user_id / character_id）反查整条身份。
     * 先查 identity_links 拿到 identityId，再回查主表（去重）。
     */
    public List<IdentityEntity> findByIdentifier(String gameId, String type, String value) {
        List<IdentityLinkEntity> links = identityLinkRepo.findByTypeAndId(type, value);
        List<IdentityEntity> result = new ArrayList<>();
        for (IdentityLinkEntity link : links) {
            identityRepo.findById(link.identityId)
                    .filter(i -> gameId == null || gameId.equals(i.gameId))
                    .filter(i -> i.deletedAt == null)
                    .ifPresent(e -> {
                        if (!result.contains(e)) result.add(e);  // 去重：同一 identityId 多 link
                    });
        }
        return result;
    }
}
