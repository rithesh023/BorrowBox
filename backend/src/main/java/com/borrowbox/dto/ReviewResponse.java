package com.borrowbox.dto;

import com.borrowbox.entity.Review;
import java.time.Instant;

public record ReviewResponse(Long id, Long itemId, String itemName, String reviewerName,
                             int rating, String comment, Instant createdAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getRental().getItem().getId(),
                review.getRental().getItem().getName(), review.getReviewer().getName(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
