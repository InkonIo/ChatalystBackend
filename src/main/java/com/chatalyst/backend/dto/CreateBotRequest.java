package com.chatalyst.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBotRequest {
    @NotBlank(message = "Название бота не может быть пустым")
    @Size(min = 3, max = 50, message = "Название бота должно быть от 3 до 50 символов")
    private String name; // Отображаемое имя бота

    @NotBlank(message = "Идентификатор бота (username) не может быть пустым")
    @Size(min = 5, max = 32, message = "Идентификатор бота должен быть от 5 до 32 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Идентификатор бота может содержать только латинские буквы, цифры и нижнее подчеркивание")
    private String botIdentifier; // Уникальное имя пользователя бота (должно заканчиваться на _bot в Telegram)

    @NotBlank(message = "Токен доступа не может быть пустым")
    @Size(max = 255, message = "Токен доступа слишком длинный")
    private String accessToken; // Токен Telegram API

    @NotBlank(message = "Платформа не может быть пустой")
    private String platform; // Например, "telegram", "whatsapp", "instagram"

    // Дополнительные поля, если нужны, например, описание бота
    private String description;
}
