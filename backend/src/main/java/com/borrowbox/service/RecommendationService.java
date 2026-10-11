package com.borrowbox.service;

import com.borrowbox.dto.ItemResponse;
import com.borrowbox.entity.*;
import com.borrowbox.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationService {
    private final ItemRepository items;
    private final ItemViewRepository views;
    private final SearchHistoryRepository searches;
    private final RentalRepository rentals;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ItemService itemService;
    private final ReviewRepository reviews;

    private static final Map<String, List<String>> RELATED = Map.of(
            "Cameras", List.of("Project Equipment", "Electronics", "Travel"),
            "Laptops", List.of("Electronics", "Project Equipment"),
            "Books", List.of("Project Equipment", "Other"),
            "Calculators", List.of("Electronics", "Books", "Project Equipment"),
            "Sports", List.of("Travel", "Other"),
            "Project Equipment", List.of("Electronics", "Cameras", "Lab Equipment"),
            "Lab Equipment", List.of("Project Equipment", "Electronics"),
            "Travel", List.of("Sports", "Cameras"));

    public RecommendationService(ItemRepository items, ItemViewRepository views, SearchHistoryRepository searches,
                                 RentalRepository rentals, UserRepository users, ItemService itemService,
                                 ReviewRepository reviews, CategoryRepository categories) {
        this.items = items; this.views = views; this.searches = searches; this.rentals = rentals;
        this.users = users; this.itemService = itemService; this.reviews = reviews; this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> forItem(long itemId, String email) {
        Item source = items.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        Set<String> preferred = preferences(email);
        Set<String> related = RELATED.getOrDefault(source.getCategory().getName(), List.of()).stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        return items.findByAvailableTrueAndDeletedFalse(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent().stream()
                .filter(candidate -> !candidate.getId().equals(itemId))
                .sorted(Comparator.comparingInt((Item item) -> score(item, related, preferred)).reversed()
                        .thenComparing(Item::getCreatedAt, Comparator.reverseOrder()))
                .limit(8).map(item -> itemService.get(item.getId())).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> forUser(long userId, String email) {
        AppUser user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own recommendations");
        Set<String> preferred = preferences(email);
        return items.findByAvailableTrueAndDeletedFalse(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent().stream()
                .filter(item -> !item.getOwner().getEmail().equalsIgnoreCase(email))
                .sorted(Comparator.comparingInt((Item item) -> score(item, Set.of(), preferred)).reversed()
                        .thenComparing(Item::getCreatedAt, Comparator.reverseOrder()))
                .limit(8).map(item -> itemService.get(item.getId())).toList();
    }

    private Set<String> preferences(String email) {
        if (email == null || email.equals("anonymousUser")) return Set.of();
        AppUser user = users.findByEmail(email).orElse(null);
        if (user == null) return Set.of();
        Set<String> result = new HashSet<>();
        views.findTop50ByUser_IdOrderByViewedAtDesc(user.getId()).forEach(view -> result.add(view.getItem().getCategory().getName().toLowerCase()));
        rentals.findByBorrower_EmailIgnoreCaseOrderByCreatedAtDesc(email).forEach(rental -> result.add(rental.getItem().getCategory().getName().toLowerCase()));
        List<String> recentSearches = searches.findTop50ByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(SearchHistory::getQuery).toList();
        for (Category category : categories.findAll()) {
            String name = category.getName().toLowerCase();
            if (recentSearches.stream().anyMatch(query -> query.toLowerCase().contains(name))) result.add(name);
        }
        return result;
    }

    private int score(Item item, Set<String> related, Set<String> preferred) {
        String category = item.getCategory().getName().toLowerCase();
        int score = 0;
        if (related.contains(category)) score += 40;
        else if (!related.isEmpty() && preferred.contains(category)) score += 30;
        else if (related.isEmpty() && preferred.contains(category)) score += 40;
        double rating = Optional.ofNullable(reviews.averageForItem(item.getId())).orElse(0.0);
        long reviewCount = reviews.countByRental_Item_Id(item.getId());
        long viewCount = views.countByItem_Id(item.getId());
        score += (int) Math.min(20, rating * 2 + Math.min(5, reviewCount * 2) + Math.min(5, viewCount / 4));
        if (item.getCreatedAt().isAfter(Instant.now().minus(30, ChronoUnit.DAYS))) score += 10;
        return score;
    }
}
