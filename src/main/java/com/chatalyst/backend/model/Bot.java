package com.chatalyst.backend.model;

import com.chatalyst.backend.Entity.User; // Убедитесь, что это правильный путь к вашей сущности User
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Отображаемое имя бота (например, "ZhetisuBot")

    @Column(nullable = false, unique = true)
    private String botIdentifier; // Уникальное имя пользователя бота (например, "ZhetisuBot_bot")

    @Column(nullable = false)
    private String platform; // Платформа (например, "telegram", "whatsapp", "instagram")

    @Column(nullable = false, length = 255) // Токен может быть длинным
    private String accessToken; // Токен Telegram API (или другой платформы)

    @Column(nullable = false, unique = true) // Добавляем ID бота, который возвращает Telegram getMe
    private Long telegramBotApiId; // ID бота, который возвращает Telegram (из getMe)

    @Column(nullable = true) // Имя магазина может быть опциональным
    private String shopName; // НОВОЕ ПОЛЕ: Имя магазина, которое будет использовать AI

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Пользователь, которому принадлежит этот бот

    @Column(length = 1000) // Увеличиваем длину для описания
    private String description; // Описание бота

    // Конструктор для создания нового бота (без shopName, platform, description по умолчанию)
    // Этот конструктор будет использоваться, если вы не используете @NoArgsConstructor и сеттеры
    public Bot(String name, String botIdentifier, String platform, String accessToken,
               Long telegramBotApiId, String shopName, User owner, String description) {
        this.name = name;
        this.botIdentifier = botIdentifier;
        this.platform = platform;
        this.accessToken = accessToken;
        this.telegramBotApiId = telegramBotApiId;
        this.shopName = shopName;
        this.owner = owner;
        this.description = description;
    }

    // Конструктор для создания нового бота (без shopName по умолчанию), как было ранее
    // Оставляем его для совместимости, но рекомендуем использовать AllArgsConstructor или NoArgsConstructor + сеттеры
    public Bot(String botIdentifier, String accessToken, String name, User owner) {
        this.botIdentifier = botIdentifier;
        this.accessToken = accessToken;
        this.name = name;
        this.owner = owner;
        this.shopName = "наш магазин"; // Значение по умолчанию
        this.platform = "telegram"; // Значение по умолчанию
        this.description = ""; // Значение по умолчанию
    }
}
