package com.chatalyst.backend.Repository;

// src/main/java/com/chatalyst/backend/repository/ChatMessageRepository.java

import com.chatalyst.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * Retrieves the top 25 chat messages for a specific chat and bot, ordered by ID in descending order.
     * This is useful for fetching the most recent messages.
     * @param chatId The ID of the chat.
     * @param botIdentifier The identifier of the bot.
     * @return A list of the top 25 ChatMessage objects.
     */
    List<ChatMessage> findTop30ByChatIdAndBotIdentifierOrderByIdDesc(Long chatId, String botIdentifier);
}