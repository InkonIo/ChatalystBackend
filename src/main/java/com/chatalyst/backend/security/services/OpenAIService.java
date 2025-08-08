package com.chatalyst.backend.security.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(WebClient openAiWebClient, ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Умный ответ с учетом истории сообщений и информации о товарах.
     * @param conversationHistory История диалога в формате List {"user", "привет"}, {"assistant", "чем могу помочь?"} и т.д.
     * @param productCatalogInfo Информация о товарах.
     * @param shopName Название магазина.
     * @return Ответ от AI.
     */
public String getBotResponse(List<String[]> chatHistory, String productCatalogInfo, String shopName) {
    ArrayNode messages = objectMapper.createArrayNode();

    // Системное сообщение: инструкция для ассистента
    ObjectNode systemMessage = objectMapper.createObjectNode();
    systemMessage.put("role", "system");
    systemMessage.put("content",
        "Ты — умный Telegram-бот-консультант, который помогает пользователю найти товары в магазине \"" + shopName + "\". " +
        "Вот информация из каталога: " + productCatalogInfo + ". " +
        "Отвечай кратко и по делу, если пользователь что-то просит — предлагай товары по смыслу. " +
        "Ты можешь догадываться, что он имеет в виду, даже если формулировка не точная. " +
        "Не выдумывай товары — только из каталога. Если ничего не найдено — мягко скажи об этом."
    );
    messages.add(systemMessage);

    // Добавляем историю сообщений (роль: user / assistant)
    for (String[] msg : chatHistory) {
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("role", msg[0]);
        messageNode.put("content", msg[1]);
        messages.add(messageNode);
    }

    // Собираем JSON-запрос
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", openaiModel);
    requestBody.set("messages", messages);
    requestBody.put("temperature", 0.7);

    log.info("⏳ Sending OpenAI request with context. Last user msg: {}", chatHistory.get(chatHistory.size() - 1)[1]);

    try {
        String responseString = openAiWebClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode rootNode = objectMapper.readTree(responseString);
        String assistantResponse = rootNode.path("choices").get(0).path("message").path("content").asText();

        log.info("✅ AI response: {}", assistantResponse);
        return assistantResponse;

    } catch (Exception e) {
        log.error("❌ OpenAI error: {}", e.getMessage(), e);
        return "Извините, произошла ошибка при обработке вашего запроса. Попробуйте позже.";
    }
}


    /**
     * Простой ответ без каталога и истории (например, для общего чата).
     */
    public String getBotResponse(String userMessage) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Ты — вежливый помощник Telegram-бота. Отвечай понятно и дружелюбно.");
            messages.add(systemMessage);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", openaiModel);
            requestBody.set("messages", messages);
            requestBody.put("temperature", 0.7);

            log.info("Sending OpenAI request (simple message): {}", userMessage);

            Mono<String> responseMono = openAiWebClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);
            String assistantResponse = rootNode.path("choices").get(0).path("message").path("content").asText();

            log.info("Received simple OpenAI response: {}", assistantResponse);
            return assistantResponse;

        } catch (Exception e) {
            log.error("Error calling OpenAI API (simple): {}", e.getMessage(), e);
            return "Извините, произошла ошибка при обработке вашего запроса.";
        }
    }
}
