package com.borrowbox.dto;

import com.borrowbox.entity.Category;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category category) { return new CategoryResponse(category.getId(), category.getName()); }
}
