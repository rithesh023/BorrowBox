package com.borrowbox.controller;

import com.borrowbox.dto.ItemRequest;
import com.borrowbox.dto.ItemResponse;
import com.borrowbox.service.ActivityService;
import com.borrowbox.service.ItemService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;
    private final ActivityService activityService;
    public ItemController(ItemService itemService, ActivityService activityService) { this.itemService = itemService; this.activityService = activityService; }

    @GetMapping
    public Page<ItemResponse> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     @RequestParam(required = false) Boolean available,
                                     @RequestParam(required = false) String condition,
                                     @RequestParam(required = false) Double minRating,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     Authentication auth) {
        if (isUser(auth)) activityService.recordSearch(auth.getName(), q == null || q.isBlank() ? category : q);
        return itemService.search(q, category, maxPrice, available, condition, minRating, page, size);
    }
    @GetMapping("/{id}") public ItemResponse get(@PathVariable long id, Authentication auth) {
        ItemResponse item = itemService.get(id);
        if (isUser(auth)) activityService.recordView(auth.getName(), id);
        return item;
    }
    @GetMapping("/mine") public Page<ItemResponse> mine(Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return itemService.mine(auth.getName(), page, size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(Authentication auth, @Valid @RequestBody ItemRequest request) {
        return itemService.create(auth.getName(), request);
    }
    @PutMapping("/{id}") public ItemResponse update(Authentication auth, @PathVariable long id, @Valid @RequestBody ItemRequest request) {
        return itemService.update(auth.getName(), id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable long id) { itemService.delete(auth.getName(), id); }

    private boolean isUser(Authentication auth) { return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()); }
}
