package com.borrowbox.service;

import com.borrowbox.dto.AdminSummary;
import com.borrowbox.entity.RentalStatus;
import com.borrowbox.repository.*;
import com.borrowbox.dto.*;
import com.borrowbox.entity.*;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    private final UserRepository users;
    private final ItemRepository items;
    private final RentalRepository rentals;
    private final PaymentRepository payments;
    private final ItemService itemService;
    public AdminService(UserRepository users, ItemRepository items, RentalRepository rentals, PaymentRepository payments, ItemService itemService) {
        this.users = users; this.items = items; this.rentals = rentals; this.payments = payments; this.itemService = itemService;
    }
    @Transactional(readOnly = true)
    public AdminSummary summary() {
        return new AdminSummary(users.count(), items.countByDeletedFalse(), rentals.countByStatus(RentalStatus.ACTIVE),
                rentals.countByStatus(RentalStatus.COMPLETED), rentals.countByStatus(RentalStatus.PENDING),
                payments.count(), payments.totalSuccessfulAmount());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() { return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(UserResponse::from).toList(); }

    @Transactional(readOnly = true)
    public List<ItemResponse> items() { return items.findByDeletedFalse(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))).stream().map(itemService::toResponse).toList(); }

    @Transactional(readOnly = true)
    public List<RentalResponse> rentals() { return rentals.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(RentalResponse::from).toList(); }

    @Transactional(readOnly = true)
    public List<PaymentResponse> payments() { return payments.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(PaymentResponse::from).toList(); }

    @Transactional
    public UserResponse setUserEnabled(long userId, boolean enabled, String adminEmail) {
        AppUser user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail)) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot disable your own account");
        user.setEnabled(enabled);
        return UserResponse.from(user);
    }

    @Transactional
    public ItemResponse setItemAvailability(long itemId, boolean available) {
        Item item = items.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (item.isDeleted()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        item.setAvailable(available);
        return itemService.toResponse(item);
    }
}
