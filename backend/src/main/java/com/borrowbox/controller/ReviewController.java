package com.borrowbox.controller;

import com.borrowbox.dto.*;
import com.borrowbox.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService) { this.reviewService = reviewService; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(Authentication auth, @Valid @RequestBody ReviewRequest request) { return reviewService.create(auth.getName(), request); }
    @GetMapping("/item/{itemId}")
    public List<ReviewResponse> byItem(@PathVariable long itemId) { return reviewService.byItem(itemId); }
    @GetMapping("/rental/{rentalId}/mine")
    public boolean hasReviewed(Authentication auth, @PathVariable long rentalId) { return reviewService.hasReviewed(auth.getName(), rentalId); }
}
