package com.app.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByCode(String code);

    List<Invitation> findByInviterId(Long inviterId);

    Optional<Invitation> findByInviteeId(Long inviteeId);

    /** 已过期但未使用的邀请码 */
    List<Invitation> findByStatusAndExpiredAtBefore(int status, LocalDateTime now);
}
