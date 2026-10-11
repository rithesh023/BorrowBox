package com.borrowbox.service;

import com.borrowbox.dto.ItemRequest;
import com.borrowbox.dto.ItemResponse;
import com.borrowbox.entity.*;
import com.borrowbox.repository.*;
import java.math.BigDecimal;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemService {
    private final ItemRepository items;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ReviewRepository reviews;
    public ItemService(ItemRepository items, UserRepository users, CategoryRepository categories, ReviewRepository reviews) {
        this.items = items; this.users = users; this.categories = categories; this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> search(String q, String category, BigDecimal maxPrice, Boolean available,
                                     String condition, Double minRating, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be 1-100");
        if (minRating != null && (minRating < 0 || minRating > 5)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating must be between 0 and 5");
        return items.search(blankToNull(q), blankToNull(category), maxPrice, available, blankToNull(condition), minRating,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ItemResponse get(long id) { return toResponse(findItem(id)); }

    @Transactional(readOnly = true)
    public Page<ItemResponse> mine(String email, int page, int size) {
        validatePage(page, size);
        return items.findByOwner_EmailIgnoreCaseAndDeletedFalse(email, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional
    public ItemResponse create(String email, ItemRequest request) {
        AppUser owner = findUser(email);
        Category category = findCategory(request.category());
        Item item = new Item(owner, category, request.name().trim(), request.description().trim(), request.pricePerDay(),
                request.securityDeposit(), request.itemCondition().trim(), request.imageUrl());
        return toResponse(items.save(item));
    }

    @Transactional
    public ItemResponse update(String email, long id, ItemRequest request) {
        Item item = findItem(id);
        requireOwner(email, item);
        return applyUpdate(item, request);
    }

    @Transactional
    public ItemResponse adminUpdate(long id, ItemRequest request) {
        return applyUpdate(findItem(id), request);
    }

    private ItemResponse applyUpdate(Item item, ItemRequest request) {
        item.update(findCategory(request.category()), request.name().trim(), request.description().trim(), request.pricePerDay(),
                request.securityDeposit(), request.itemCondition().trim(), request.imageUrl(),
                request.available() == null ? item.isAvailable() : request.available());
        return toResponse(item);
    }

    @Transactional
    public void delete(String email, long id) {
        Item item = findItem(id);
        requireOwner(email, item);
        // Keep rental/payment/review references intact while removing the listing from marketplace views.
        item.markDeleted();
    }

    @Transactional
    public void adminDelete(long id) {
        findItem(id).markDeleted();
    }

    private Item findItem(long id) {
        Item item = items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (item.isDeleted()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        return item;
    }
    private AppUser findUser(String email) { return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found")); }
    private Category findCategory(String name) { return categories.findByNameIgnoreCase(name.trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category")); }
    private void requireOwner(String email, Item item) {
        if (!item.getOwner().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the item owner can change this listing");
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public ItemResponse toResponse(Item item) {
        Double average = reviews.averageForItem(item.getId());
        return ItemResponse.from(item, average == null ? 0 : average, reviews.countByRental_Item_Id(item.getId()));
    }
    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be 1-100");
    }
}
