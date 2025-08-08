// src/main/java/com/chatalyst/backend/security/services/ProductService.java
package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.Repository.ProductRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.CreateProductRequest;
import com.chatalyst.backend.dto.ProductResponse;
import com.chatalyst.backend.dto.UpdateProductRequest;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final BotRepository botRepository;
    private final UserRepository userRepository; // Для проверки владельца

    /**
     * Создает новый товар и привязывает его к указанному боту.
     * @param request DTO с данными для создания товара.
     * @param userId ID пользователя, создающего товар (для проверки прав).
     * @return Созданный объект ProductResponse.
     * @throws RuntimeException если бот не найден или пользователь не является владельцем бота.
     */
    public ProductResponse createProduct(CreateProductRequest request, Long userId) {
        Bot bot = botRepository.findById(request.getBotId())
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + request.getBotId()));
        // Проверка прав
        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для добавления товаров в этого бота.");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setCatalog(request.getCatalog());
        product.setSubcategory(request.getSubcategory());
        product.setImageUrl(request.getImageUrl());
        product.setInStock(request.isInStock());
        product.setBot(bot);

        Product savedProduct = productRepository.save(product);
        return convertToResponse(savedProduct);
    }

    /**
     * Обновляет существующий товар.
     * @param productId ID товара для обновления.
     * @param request DTO с обновленными данными.
     * @param userId ID пользователя, обновляющего товар (для проверки прав).
     * @return Обновленный объект ProductResponse.
     * @throws RuntimeException если товар не найден или пользователь не является владельцем бота.
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден с ID: " + productId));

        // Проверка прав через бота
        if (!product.getBot().getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для обновления этого товара.");
        }

        Optional.ofNullable(request.getName()).ifPresent(product::setName);
        Optional.ofNullable(request.getPrice()).ifPresent(product::setPrice);
        Optional.ofNullable(request.getDescription()).ifPresent(product::setDescription);
        Optional.ofNullable(request.getCatalog()).ifPresent(product::setCatalog);
        Optional.ofNullable(request.getSubcategory()).ifPresent(product::setSubcategory);
        Optional.ofNullable(request.getImageUrl()).ifPresent(product::setImageUrl);
        product.setInStock(request.isInStock()); // Обновляем статус наличия

        Product updatedProduct = productRepository.save(product);
        return convertToResponse(updatedProduct);
    }

    /**
     * Удаляет товар.
     * @param productId ID товара для удаления.
     * @param userId ID пользователя, удаляющего товар (для проверки прав).
     * @throws RuntimeException если товар не найден или пользователь не является владельцем бота.
     */
    public void deleteProduct(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден с ID: " + productId));

        if (!product.getBot().getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для удаления этого товара.");
        }

        productRepository.delete(product);
    }

    /**
     * Получает товар по ID.
     * @param productId ID товара.
     * @return Объект ProductResponse.
     * @throws RuntimeException если товар не найден.
     */
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден с ID: " + productId));
        return convertToResponse(product);
    }

    /**
     * Получает список всех товаров для определенного бота.
     * @param botId ID бота.
     * @param userId ID пользователя (для проверки прав).
     * @return Список объектов ProductResponse.
     * @throws RuntimeException если бот не найден или пользователь не является владельцем бота.
     */
    public List<ProductResponse> getProductsByBotId(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для просмотра товаров этого бота.");
        }

        List<Product> products = productRepository.findByBot(bot);
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получает список уникальных каталогов для бота.
     * @param botId ID бота.
     * @return Список названий каталогов.
     */
    public List<String> getUniqueCatalogsByBot(Long botId) {
        return productRepository.findUniqueCatalogsByBotId(botId);
    }

    /**
     * Получает список уникальных подкаталогов для определенного каталога и бота.
     * @param botId ID бота.
     * @param catalog Название каталога.
     * @return Список названий подкаталогов.
     */
    public List<String> getUniqueSubcategoriesByBotAndCatalog(Long botId, String catalog) {
        return productRepository.findUniqueSubcategoriesByBotIdAndCatalog(botId, catalog);
    }

    /**
     * Получает список товаров по подкаталогу, которые есть в наличии.
     * @param botId ID бота.
     * @param catalog Название каталога.
     * @param subcategory Название подкаталога.
     * @return Список объектов ProductResponse.
     */
    public List<ProductResponse> getProductsByBotAndSubcategoryInStock(Long botId, String catalog, String subcategory) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));
        List<Product> products = productRepository.findByBotAndCatalogAndSubcategoryAndInStock(bot, catalog, subcategory, true);
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Находит товар по имени и боту, который есть в наличии.
     * @param bot ID бота.
     * @param productName Название товара.
     * @return Optional с товаром.
     */
    public Optional<Product> findProductByNameAndBotInStock(Bot bot, String productName) {
        return productRepository.findByNameAndBotAndInStock(productName, bot, true);
    }


    /**
     * Вспомогательный метод для преобразования сущности Product в DTO ProductResponse.
     * @param product Сущность Product.
     * @return DTO ProductResponse.
     */
    private ProductResponse convertToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setDescription(product.getDescription());
        response.setCatalog(product.getCatalog());
        response.setSubcategory(product.getSubcategory());
        response.setImageUrl(product.getImageUrl());
        response.setInStock(product.isInStock());
        response.setBotId(product.getBot().getId());
        return response;
    }
}
