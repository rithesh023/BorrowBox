package com.borrowbox.service;

import com.borrowbox.dto.CategoryResponse;
import com.borrowbox.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() { return categories.findAll().stream().map(CategoryResponse::from).toList(); }
}
