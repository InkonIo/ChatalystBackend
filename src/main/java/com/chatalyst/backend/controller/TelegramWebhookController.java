package com.chatalyst.backend.controller;


import com.chatalyst.backend.security.services.TelegramService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Telegram Webhook", description = "Endpoints for Telegram bot webhook integration")
public class TelegramWebhookController {

    private final TelegramService telegramService;

    /**
     * Эндпоинт для приема входящих обновлений от Telegram.
     * Telegram будет отправлять сюда POST-запросы при каждом новом сообщении или другом событии.
     * @param updateJson JSON-объект, содержащий информацию об обновлении.
     * @return HTTP 200 OK, чтобы Telegram знал, что обновление обработано.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Receive Telegram webhook updates", description = "This endpoint receives all updates from Telegram bot API.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<Void> handleTelegramWebhook(@RequestBody JsonNode updateJson) {
        log.info("Received Telegram webhook. Processing update...");
        telegramService.processUpdate(updateJson);
        return ResponseEntity.ok().build(); // Всегда возвращаем 200 OK, чтобы Telegram не повторял отправку
    }
}
