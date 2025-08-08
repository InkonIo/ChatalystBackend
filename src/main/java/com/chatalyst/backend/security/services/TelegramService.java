// src/main/java/com/chatalyst/backend/security/services/TelegramService.java
package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.*;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Product;
import com.chatalyst.backend.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для взаимодействия с Telegram Bot API.
 */
@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String defaultBotToken;

    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;
    private final BotRepository botRepository;
    private final ProductRepository productRepository;
    private final ChatMessageRepository chatMessageRepository; // Добавляем репозиторий для сообщений

    @Qualifier("telegramWebClient")
    private final WebClient telegramWebClient;

    public TelegramService(ObjectMapper objectMapper, OpenAIService openAIService,
                           BotRepository botRepository, ProductRepository productRepository,
                           ChatMessageRepository chatMessageRepository,
                           WebClient telegramWebClient) {
        this.objectMapper = objectMapper;
        this.openAIService = openAIService;
        this.botRepository = botRepository;
        this.productRepository = productRepository;
        this.chatMessageRepository = chatMessageRepository; // Инициализируем репозиторий
        this.telegramWebClient = telegramWebClient;
    }

    /**
     * Основной метод для обработки всех входящих обновлений от Telegram.
     * @param botIdentifier Идентификатор бота.
     * @param updateJson JSON-объект входящего обновления.
     */
    public void processUpdate(String botIdentifier, JsonNode updateJson) {
        if (updateJson.has("message")) {
            JsonNode message = updateJson.get("message");
            long chatId = message.get("chat").get("id").asLong();
            String text = message.has("text") ? message.get("text").asText() : "";
            log.info("Received message for bot {} from chat {}: {}", botIdentifier, chatId, text);

            // Проверяем, что это команда
            if (text.startsWith("/")) {
                handleCommand(botIdentifier, chatId, text);
            } else {
                // Если это не команда, отправляем на обработку в AI
                sendOpenAIResponse(botIdentifier, chatId, text);
            }
        }
    }

    /**
     * Обрабатывает команды, начинающиеся с "/".
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата.
     * @param command Текст команды.
     */
    private void handleCommand(String botIdentifier, long chatId, String command) {
        log.info("Processing command for bot {}: {}", botIdentifier, command);

        Optional<Bot> botOptional = botRepository.findByBotIdentifier(botIdentifier);
        if (botOptional.isEmpty()) {
            sendMessage(chatId, "Бот с таким идентификатором не найден.", defaultBotToken);
            return;
        }
        Bot bot = botOptional.get();

        if (command.startsWith("/start")) {
            sendMessage(chatId, "Добро пожаловать в магазин \"" + bot.getShopName() + "\"! Напишите /catalog, чтобы увидеть категории товаров.", bot.getAccessToken());

        } else if (command.equals("/catalog")) {
            sendCatalogList(chatId, bot);

        } else if (command.startsWith("/catalog_")) {
            String catalog = command.substring("/catalog_".length());
            sendSubcategoriesFromCatalog(chatId, bot, catalog);

        } else if (command.startsWith("/subcategory_")) {
            String subcategory = command.substring("/subcategory_".length());
            sendSubcategoryProducts(chatId, bot, subcategory);

        } else {
            sendMessage(chatId, "Неизвестная команда. Пожалуйста, используйте /start или /catalog.", bot.getAccessToken());
        }
    }

    /**
     * Отправляет список доступных каталогов.
     * @param chatId ID чата.
     * @param bot Объект бота.
     */
    private void sendCatalogList(long chatId, Bot bot) {
        List<String> catalogs = productRepository.findByBot(bot)
                .stream()
                .map(Product::getCatalog)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (catalogs.isEmpty()) {
            sendMessage(chatId, "В магазине нет доступных каталогов.", bot.getAccessToken());
            return;
        }

        StringBuilder messageBuilder = new StringBuilder("Каталоги магазина \"" + bot.getShopName() + "\":\n\n");
        catalogs.forEach(catalog -> messageBuilder.append("/catalog_").append(catalog).append("\n"));
        sendMessage(chatId, messageBuilder.toString(), bot.getAccessToken());
    }

    /**
     * Отправляет список подкатегорий из определенного каталога.
     * @param chatId ID чата.
     * @param bot Объект бота.
     * @param catalog Название каталога.
     */
    private void sendSubcategoriesFromCatalog(long chatId, Bot bot, String catalog) {
        List<String> subcategories = productRepository.findByBotAndCatalog(bot, catalog)
                .stream()
                .map(Product::getSubcategory)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (subcategories.isEmpty()) {
            sendMessage(chatId, "В каталоге \"" + catalog + "\" нет подкаталогов.", bot.getAccessToken());
            return;
        }

        StringBuilder messageBuilder = new StringBuilder("Подкаталоги в каталоге \"" + catalog + "\":\n\n");
        subcategories.forEach(subcat -> messageBuilder.append("/subcategory_").append(subcat).append("\n"));
        sendMessage(chatId, messageBuilder.toString(), bot.getAccessToken());
    }

    /**
     * Отправляет список товаров из подкаталога.
     * @param chatId ID чата.
     * @param bot Объект бота.
     * @param subcategory Название подкаталога.
     */
    private void sendSubcategoryProducts(long chatId, Bot bot, String subcategory) {
        List<Product> products = productRepository.findByBotAndSubcategory(bot, subcategory);
        if (products.isEmpty()) {
            sendMessage(chatId, "В подкаталоге \"" + subcategory + "\" нет товаров.", bot.getAccessToken());
            return;
        }

        StringBuilder messageBuilder = new StringBuilder("Товары в категории \"" + subcategory + "\":\n\n");
        products.forEach(product -> messageBuilder.append("- ").append(product.getName()).append(" (").append(product.getPrice()).append(" руб.)\n"));
        sendMessage(chatId, messageBuilder.toString(), bot.getAccessToken());
    }

    /**
     * Отправляет сообщение пользователя в AI для обработки.
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата.
     * @param userMessage Сообщение пользователя.
     */
    private void sendOpenAIResponse(String botIdentifier, long chatId, String userMessage) {
        Optional<Bot> botOptional = botRepository.findByBotIdentifier(botIdentifier);
        if (botOptional.isEmpty()) {
            sendMessage(chatId, "Бот с таким идентификатором не найден.", defaultBotToken);
            return;
        }
        Bot bot = botOptional.get();

        // 1. Получаем историю диалога
        List<ChatMessage> history = chatMessageRepository.findTop30ByChatIdAndBotIdentifierOrderByIdDesc(chatId, botIdentifier);

        // 2. Формируем список истории для AI.
        List<String[]> chatHistory = history.stream()
                .map(m -> new String[]{m.getRole(), m.getContent()})
                .collect(Collectors.toList());

        // Добавляем текущее сообщение пользователя в конец истории
        chatHistory.add(new String[]{"user", userMessage});

        // 3. Формируем информацию о товарах для AI
        String productCatalogInfo = productRepository.findByBot(bot).stream()
                .collect(Collectors.groupingBy(Product::getCatalog))
                .entrySet().stream()
                .map(entry -> {
                    String catalog = entry.getKey();
                    return "Каталог: " + catalog + "\n" +
                            entry.getValue().stream()
                                    .collect(Collectors.groupingBy(Product::getSubcategory))
                                    .entrySet().stream()
                                    .map(subEntry -> {
                                        String subcategory = subEntry.getKey();
                                        String products = subEntry.getValue().stream()
                                                .map(p -> "- " + p.getName() + " (" + p.getPrice() + " руб.): " + p.getDescription())
                                                .collect(Collectors.joining("\n"));
                                        return "  Подкаталог: " + subcategory + "\n" + products;
                                    }).collect(Collectors.joining("\n"));
                }).collect(Collectors.joining("\n\n"));

        // 4. Получаем ответ от AI
        String aiResponse = openAIService.getBotResponse(chatHistory, productCatalogInfo, bot.getShopName());

        // 5. Сохраняем новое сообщение в историю
        ChatMessage userMsg = ChatMessage.builder()
                .chatId(chatId)
                .botIdentifier(botIdentifier)
                .role("user")
                .content(userMessage)
                .build();

        ChatMessage aiMsg = ChatMessage.builder()
                .chatId(chatId)
                .botIdentifier(botIdentifier)
                .role("assistant")
                .content(aiResponse)
                .build();

        chatMessageRepository.save(userMsg);
        chatMessageRepository.save(aiMsg);

        // 6. Отправляем ответ
        sendMessage(chatId, aiResponse, bot.getAccessToken());
    }

    /**
     * Sends a text message to a specific Telegram chat using a designated bot token.
     * This is the corrected method to fix the 400 Bad Request error.
     * @param chatId The ID of the chat to which the message will be sent.
     * @param text The message text to send.
     * @param botAccessToken The access token for the bot.
     */
    public void sendMessage(long chatId, String text, String botAccessToken) {
        // Build the request body as a JSON object using ObjectMapper
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);

        log.info("Sending message to Telegram chat {} with text: '{}' using bot token: {}", chatId, text, botAccessToken);

        try {
            Mono<String> responseMono = telegramWebClient.post()
                    .uri(String.format("/bot%s/sendMessage", botAccessToken))
                    .bodyValue(requestBody) // Send the ObjectNode directly
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                log.error("Failed to send message to Telegram chat {}: {}", chatId, rootNode.path("description").asText());
            } else {
                log.info("Message sent successfully to chat {}", chatId);
            }
        } catch (WebClientResponseException e) {
            log.error("Failed to send message to Telegram chat {}: {} - Response body: {}", chatId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет фотографию в Telegram.
     * @param chatId ID чата.
     * @param photoUrl URL фотографии.
     * @param caption Подпись к фотографии.
     * @param botAccessToken Токен доступа бота.
     */
    // (Remaining methods like sendPhoto and others would go here)
    // The sendPhoto method you provided is already in good shape for JSON handling.
    // It is omitted here for brevity.
    public void sendPhoto(long chatId, String photoUrl, String caption, String botAccessToken) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("photo", photoUrl);
        if (caption != null && !caption.isEmpty()) {
            requestBody.put("caption", caption);
        }
        log.info("Sending photo to Telegram chat {} with URL: {} using bot token: {}", chatId, photoUrl, botAccessToken);
        try {
            Mono<String> responseMono = telegramWebClient.post()
                    .uri(String.format("/bot%s/sendPhoto", botAccessToken))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);
            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);
            if (!rootNode.path("ok").asBoolean()) {
                log.error("Failed to send photo to Telegram chat {}: {}", chatId, rootNode.path("description").asText());
            } else {
                log.info("Photo sent successfully to chat {}", chatId);
            }
        } catch (Exception e) {
            log.error("Failed to send photo to Telegram chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    // Перегруженный метод для обратной совместимости или сообщений, не требующих конкретного бота
    public void sendMessage(long chatId, String text) {
        if (defaultBotToken == null || defaultBotToken.isEmpty()) {
            log.error("Default Telegram bot token is not configured. Cannot send message without a specific bot token.");
            return;
        }
        sendMessage(chatId, text, defaultBotToken);
    }
}