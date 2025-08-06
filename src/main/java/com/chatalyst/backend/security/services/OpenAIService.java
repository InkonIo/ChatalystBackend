package com.chatalyst.backend.security.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Qualifier("openAiWebClient") // Указываем, какой именно WebClient инжектировать
    private final WebClient openAiWebClient; // <-- Инжектированный WebClient
    private final ObjectMapper objectMapper;

    // Конструктор теперь инжектирует WebClient и ObjectMapper
    public OpenAIService(WebClient openAiWebClient, ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Отправляет запрос на Chat Completion API OpenAI и возвращает ответ.
     * @param userMessage Сообщение пользователя.
     * @param productCatalogPrompt Часть промпта с информацией о товарах (пока заглушка, позже будет из БД).
     * @return Ответ от OpenAI.
     */
    public String getChatCompletion(String userMessage, String productCatalogPrompt) {
        // Создаем системный промпт, который будет содержать информацию о товарах
        String systemPrompt = "Ты AI-продавец для магазина Chatalyst. Отвечай на вопросы клиентов о товарах. " +
                              "Используй следующую информацию о товарах: " + productCatalogPrompt +
                              "Если информации о товаре нет, вежливо сообщи, что не можешь ответить на этот вопрос.";

        ObjectNode message1 = objectMapper.createObjectNode();
        message1.put("role", "system");
        message1.put("content", systemPrompt);

        ObjectNode message2 = objectMapper.createObjectNode();
        message2.put("role", "user");
        message2.put("content", userMessage);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message1);
        messages.add(message2);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openaiModel);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.7); // Настройка креативности ответа

        log.info("Sending OpenAI request for user message: {}", userMessage);

        try {
            // Отправляем запрос к OpenAI API
            Mono<String> responseMono = openAiWebClient.post() // <-- Используем инжектированный openAiWebClient
                    .uri("/chat/completions") // <-- URI без базового хоста
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);
            String assistantResponse = rootNode.path("choices").get(0).path("message").path("content").asText();

            log.info("Received OpenAI response: {}", assistantResponse);
            return assistantResponse;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return "Извините, произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте позже.";
        }
    }
}
