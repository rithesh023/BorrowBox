package com.borrowbox.controller;

import com.borrowbox.dto.CategoryResponse;
import com.borrowbox.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }
    @GetMapping
    public List<CategoryResponse> list() { return categoryService.list(); }
}
