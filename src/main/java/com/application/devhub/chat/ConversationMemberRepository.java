package com.application.devhub.chat;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.Key> {

    @Query("select m from ConversationMember m where m.key.conversationId = :conversationId and m.key.userId = :userId")
    Optional<ConversationMember> find(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from ConversationMember m where m.key.conversationId = :conversationId and m.key.userId = :userId")
    Optional<ConversationMember> findForUpdate(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Query("select count(m) from ConversationMember m where m.key.conversationId = :conversationId and m.status = com.application.devhub.chat.MemberStatus.ACTIVE")
    long countActive(@Param("conversationId") UUID conversationId);

    @Query("""
            select m from ConversationMember m
            where m.key.conversationId = :conversationId and m.status = com.application.devhub.chat.MemberStatus.ACTIVE
            order by m.joinedAt, m.key.userId
            """)
    List<ConversationMember> findActive(@Param("conversationId") UUID conversationId);

    @Query("""
            select m from ConversationMember m
            where m.key.conversationId = :conversationId
              and m.status in (com.application.devhub.chat.MemberStatus.ACTIVE, com.application.devhub.chat.MemberStatus.REQUEST)
            """)
    List<ConversationMember> findReachable(@Param("conversationId") UUID conversationId);
}
