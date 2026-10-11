package com.borrowbox.controller;

import com.borrowbox.dto.ItemResponse;
import com.borrowbox.service.RecommendationService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendations;
    public RecommendationController(RecommendationService recommendations) { this.recommendations = recommendations; }

    @GetMapping("/item/{itemId}")
    public List<ItemResponse> forItem(@PathVariable long itemId, Authentication auth) {
        return recommendations.forItem(itemId, auth == null ? null : auth.getName());
    }

    @GetMapping("/user/{userId}")
    public List<ItemResponse> forUser(@PathVariable long userId, Authentication auth) {
        return recommendations.forUser(userId, auth.getName());
    }
}
