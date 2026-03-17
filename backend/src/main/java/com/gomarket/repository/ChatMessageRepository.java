package com.gomarket.repository;

import com.gomarket.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.requestId = :requestId AND cm.id > :afterId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findNewMessages(Long requestId, Long afterId);

    /** Get distinct request IDs where this user has chat messages, ordered by latest message */
    @Query("SELECT DISTINCT cm.requestId FROM ChatMessage cm WHERE cm.senderId = :userId OR cm.receiverId = :userId ORDER BY cm.requestId DESC")
    List<Long> findConversationRequestIds(Long userId);

    /** Get the latest message for a given request */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.requestId = :requestId ORDER BY cm.createdAt DESC LIMIT 1")
    ChatMessage findLatestByRequestId(Long requestId);
}
