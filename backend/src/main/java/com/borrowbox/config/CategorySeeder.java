package com.borrowbox.config;

import com.borrowbox.entity.Category;
import com.borrowbox.repository.CategoryRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategorySeeder {
    @Bean
    CommandLineRunner seedCategories(CategoryRepository categories) {
        return args -> List.of("Books", "Electronics", "Cameras", "Laptops", "Calculators", "Sports",
                        "Project Equipment", "Lab Equipment", "Travel", "Other")
                .forEach(name -> categories.findByNameIgnoreCase(name).orElseGet(() -> categories.save(new Category(name))));
    }
}
