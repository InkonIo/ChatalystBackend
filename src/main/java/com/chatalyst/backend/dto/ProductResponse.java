// src/main/java/com/chatalyst/backend/dto/ProductResponse.java
package com.chatalyst.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private String catalog; // Каталог товара
    private String subcategory; // Подкаталог товара
    private String imageUrl;
    private boolean inStock; // Статус наличия товара
    private Long botId; // ID бота, к которому принадлежит товар
}
