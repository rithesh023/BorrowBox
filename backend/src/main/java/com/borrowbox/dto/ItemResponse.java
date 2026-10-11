package com.borrowbox.dto;

import com.borrowbox.entity.Item;
import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponse(Long id, Long ownerId, String ownerName, String category, String name,
                           String description, BigDecimal pricePerDay, BigDecimal securityDeposit,
                           String itemCondition, String imageUrl, boolean available, Instant createdAt,
                           double averageRating, long reviewCount) {
    public static ItemResponse from(Item item, double averageRating, long reviewCount) {
        return new ItemResponse(item.getId(), item.getOwner().getId(), item.getOwner().getName(),
                item.getCategory().getName(), item.getName(), item.getDescription(), item.getPricePerDay(),
                item.getSecurityDeposit(), item.getItemCondition(), item.getImageUrl(), item.isAvailable(), item.getCreatedAt(),
                averageRating, reviewCount);
    }
}
