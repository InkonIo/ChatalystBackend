package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.Repository.ProductRepository;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier; // Импортируем Qualifier
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TelegramService {

    // Этот токен будет использоваться для инициализации WebClient, но не для каждого запроса
    @Value("${telegram.bot.token}")
    private String defaultBotToken; // Переименован для ясности, так как будем использовать токены из БД

    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;
    private final BotRepository botRepository;
    private final ProductRepository productRepository;

    @Qualifier("telegramWebClient") // Указываем, какой именно WebClient инжектировать
    private final WebClient telegramWebClient; // Инжектируем WebClient из WebClientConfig

    // Конструктор теперь инжектирует все необходимые зависимости
    public TelegramService(ObjectMapper objectMapper, OpenAIService openAIService,
                           BotRepository botRepository, ProductRepository productRepository,
                           @Qualifier("telegramWebClient") WebClient telegramWebClient) {
        this.objectMapper = objectMapper;
        this.openAIService = openAIService;
        this.botRepository = botRepository;
        this.productRepository = productRepository;
        this.telegramWebClient = telegramWebClient; // Инжектируем WebClient
    }

    // Удаляем @PostConstruct init(), так как WebClient теперь инжектируется

    /**
     * Обрабатывает входящее обновление от Telegram.
     * @param updateJson JSON-объект входящего обновления от Telegram.
     */
    public void processUpdate(JsonNode updateJson) {
        log.info("Received Telegram update: {}", updateJson.toString());

        JsonNode messageNode = updateJson.path("message");
        if (messageNode.isMissingNode()) {
            log.warn("Update does not contain a message node. Skipping.");
            return;
        }

        long chatId = messageNode.path("chat").path("id").asLong();
        String text = messageNode.path("text").asText();
        JsonNode fromNode = messageNode.path("from");
        Long telegramBotApiId = fromNode.path("id").asLong(); // ID бота, от которого пришло сообщение (если это бот)

        if (text.isEmpty()) {
            log.warn("Message text is empty. Skipping.");
            return;
        }

        log.info("Processing message from chat {}: {}", chatId, text);

        // 1. Находим бота по его telegramBotApiId
        Optional<Bot> optionalBot = botRepository.findByTelegramBotApiId(telegramBotApiId);
        if (optionalBot.isEmpty()) {
            log.warn("Бот с Telegram API ID {} не найден в нашей системе. Не могу обработать сообщение.", telegramBotApiId);
            sendMessage(chatId, "Извините, этот бот не зарегистрирован в нашей системе.");
            return;
        }
        Bot bot = optionalBot.get();

        // 2. Получаем товары, привязанные к этому боту
        List<Product> products = productRepository.findByBot(bot);
        String productCatalogInfo = formatProductsForAI(products);

        // 3. Получаем ответ от OpenAI, используя токен конкретного бота
        String openAIResponse = openAIService.getChatCompletion(text, productCatalogInfo);

        // 4. Отправляем ответ обратно в Telegram, используя токен конкретного бота
        sendMessage(chatId, openAIResponse, bot.getAccessToken());
    }

    /**
     * Форматирует список товаров в строку для использования в промпте OpenAI.
     * @param products Список товаров.
     * @return Строка с описанием товаров.
     */
    private String formatProductsForAI(List<Product> products) {
        if (products.isEmpty()) {
            return "В данный момент нет информации о товарах.";
        }
        return products.stream()
                .map(p -> String.format("%s (цена %s тг, описание: %s)",
                        p.getName(), p.getPrice(), p.getDescription()))
                .collect(Collectors.joining("; "));
    }

    /**
     * Отправляет сообщение в указанный чат Telegram, используя токен конкретного бота.
     * @param chatId ID чата, куда отправить сообщение.
     * @param text Текст сообщения.
     * @param botAccessToken Токен доступа конкретного бота.
     */
    public void sendMessage(long chatId, String text, String botAccessToken) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);

        log.info("Sending message to Telegram chat {} using bot token: {}", chatId, botAccessToken);

        try {
            // ИСПОЛЬЗУЕМ ИНЖЕКТИРОВАННЫЙ telegramWebClient и добавляем токен в URI
            Mono<String> responseMono = telegramWebClient.post()
                    .uri("bot" + botAccessToken + "/sendMessage") // Токен теперь часть URI, а не baseUrl
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                log.error("Failed to send message to Telegram chat {}: {}", chatId, rootNode.path("description").asText());
            } else {
                log.info("Message sent successfully to chat {}", chatId);
            }
        } catch (Exception e) {
            log.error("Failed to send message to Telegram chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    // Перегруженный метод для обратной совместимости или сообщений, не требующих конкретного бота
    public void sendMessage(long chatId, String text) {
        // Используем дефолтный токен, если он есть, или выбрасываем ошибку
        if (defaultBotToken == null || defaultBotToken.isEmpty()) {
            log.error("Default Telegram bot token is not configured. Cannot send message without a specific bot token.");
            return;
        }
        sendMessage(chatId, text, defaultBotToken);
    }
}