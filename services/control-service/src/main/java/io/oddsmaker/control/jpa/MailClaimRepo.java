package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MailClaimRepo extends JpaRepository<MailClaimEntity, String> {

    Optional<MailClaimEntity> findByMailIdAndPlayerKey(String mailId, String playerKey);
}
