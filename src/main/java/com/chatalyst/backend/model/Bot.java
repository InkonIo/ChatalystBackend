package com.chatalyst.backend.model;

import com.chatalyst.backend.Entity.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Пользователь, которому принадлежит этот бот

    private String description; // Описание бота

    // Возможно, позже добавим поля для статуса бота (активен/неактивен), даты создания и т.д.
}
