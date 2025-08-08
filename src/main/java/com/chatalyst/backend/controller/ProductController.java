// src/main/java/com/chatalyst/backend/controller/ProductController.java
package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.CreateProductRequest;
import com.chatalyst.backend.dto.MessageResponse;
import com.chatalyst.backend.dto.ProductResponse;
import com.chatalyst.backend.dto.UpdateProductRequest;
import com.chatalyst.backend.security.services.ProductService;
import com.chatalyst.backend.security.services.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "API для управления товарами")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Создать новый товар", description = "Создает новый товар, привязанный к боту.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар успешно создан",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductRequest request,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            ProductResponse product = productService.createProduct(request, userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (RuntimeException e) {
            log.error("Ошибка при создании товара для пользователя {}: {}", userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Обновить товар", description = "Обновляет существующий товар по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар успешно обновлен",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            ProductResponse updatedProduct = productService.updateProduct(id, request, userPrincipal.getId());
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении товара с ID {} для пользователя {}: {}", id, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Удалить товар", description = "Удаляет товар по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар успешно удален"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            productService.deleteProduct(id, userPrincipal.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении товара с ID {} для пользователя {}: {}", id, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить товар по ID", description = "Возвращает информацию о товаре по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар успешно получен",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении товара с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }


    @GetMapping("/bot/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить товары бота", description = "Возвращает все товары, привязанные к указанному боту.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно получены",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getProductsByBotId(@PathVariable Long botId,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<ProductResponse> products = productService.getProductsByBotId(botId, userPrincipal.getId());
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении товаров для бота с ID {} для пользователя {}: {}", botId, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    // Новый эндпоинт для получения уникальных каталогов
    @GetMapping("/bot/{botId}/catalogs")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить уникальные каталоги бота", description = "Возвращает список уникальных названий каталогов для указанного бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Каталоги успешно получены",
                    content = @Content(schema = @Schema(implementation = String.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    public ResponseEntity<List<String>> getUniqueCatalogsByBotId(@PathVariable Long botId) {
        List<String> catalogs = productService.getUniqueCatalogsByBot(botId);
        return ResponseEntity.ok(catalogs);
    }

    // Новый эндпоинт для получения уникальных подкаталогов
    @GetMapping("/bot/{botId}/catalogs/{catalog}/subcategories")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить уникальные подкаталоги", description = "Возвращает список уникальных названий подкаталогов для указанного каталога и бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подкаталоги успешно получены",
                    content = @Content(schema = @Schema(implementation = String.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    public ResponseEntity<List<String>> getUniqueSubcategoriesByBotAndCatalog(
            @PathVariable Long botId, @PathVariable String catalog) {
        List<String> subcategories = productService.getUniqueSubcategoriesByBotAndCatalog(botId, catalog);
        return ResponseEntity.ok(subcategories);
    }

    // Новый эндпоинт для получения товаров по подкаталогу (только в наличии)
    @GetMapping("/bot/{botId}/catalogs/{catalog}/subcategories/{subcategory}/instock")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить товары в наличии по подкаталогу", description = "Возвращает список товаров, которые есть в наличии, для указанного подкаталога.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно получены",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getProductsByBotAndSubcategoryInStock(
            @PathVariable Long botId, @PathVariable String catalog, @PathVariable String subcategory) {
        try {
            List<ProductResponse> products = productService.getProductsByBotAndSubcategoryInStock(botId, catalog, subcategory);
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении товаров для бота с ID {} по каталогу {} и подкатегории {}: {}", botId, catalog, subcategory, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }
}
