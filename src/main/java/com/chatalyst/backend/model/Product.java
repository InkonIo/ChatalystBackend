package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal; // Для точного хранения цен

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2) // Цена с двумя знаками после запятой
    private BigDecimal price;

    @Column(length = 1000) // Увеличиваем длину для описания
    private String description;

    private String imageUrl; // URL к изображению товара

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot; // Бот, которому принадлежит этот товар

    // Возможно, позже добавим поле для статуса товара (в наличии/нет в наличии) и т.д.
}

