package com.chatalyst.backend.security.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct; // Импортируем PostConstruct
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private WebClient webClient; // Удаляем final
    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;

    // Конструктор теперь инжектирует ObjectMapper и OpenAIService
    public TelegramService(ObjectMapper objectMapper, OpenAIService openAIService) {
        this.objectMapper = objectMapper;
        this.openAIService = openAIService;
    }

    // Инициализируем WebClient после инжекции @Value полей
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken + "/")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Обрабатывает входящее обновление от Telegram.
     * @param updateJson JSON-объект входящего обновления от Telegram.
     */
    public void processUpdate(JsonNode updateJson) {
        log.info("Received Telegram update: {}", updateJson.toString());

        // Извлекаем chat_id и text из сообщения
        JsonNode messageNode = updateJson.path("message");
        if (messageNode.isMissingNode()) {
            log.warn("Update does not contain a message node. Skipping.");
            return;
        }

        long chatId = messageNode.path("chat").path("id").asLong();
        String text = messageNode.path("text").asText();

        if (text.isEmpty()) {
            log.warn("Message text is empty. Skipping.");
            return;
        }

        log.info("Processing message from chat {}: {}", chatId, text);

        // TODO: Здесь будет логика определения botId из /start payload и связь с товарами юзера
        // Пока что используем заглушку для информации о товарах.
        String productCatalogInfo = "Наши товары: Смартфон (цена 50000 тг, описание: мощный, камера 108МП), " +
                                    "Ноутбук (цена 250000 тг, описание: легкий, быстрый процессор), " +
                                    "Наушники (цена 15000 тг, описание: беспроводные, с шумоподавлением).";


        // Получаем ответ от OpenAI
        String openAIResponse = openAIService.getChatCompletion(text, productCatalogInfo);

        // Отправляем ответ обратно в Telegram
        sendMessage(chatId, openAIResponse);
    }

    /**
     * Отправляет сообщение в указанный чат Telegram.
     * @param chatId ID чата, куда отправить сообщение.
     * @param text Текст сообщения.
     */
    public void sendMessage(long chatId, String text) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);

        log.info("Sending message to Telegram chat {}: {}", chatId, text);

        try {
            webClient.post()
                    .uri("/sendMessage")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Блокирующий вызов для простоты MVP
            log.info("Message sent successfully to chat {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
