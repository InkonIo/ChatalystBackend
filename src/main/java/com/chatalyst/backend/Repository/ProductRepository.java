package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Product;
import com.chatalyst.backend.model.Bot; // Убедитесь, что путь к вашей сущности Bot правильный
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Найти все товары, привязанные к определенному боту
    List<Product> findByBot(Bot bot);

    // Найти товар по имени и боту (для поиска в промпте)
    Optional<Product> findByNameAndBot(String name, Bot bot);
}
