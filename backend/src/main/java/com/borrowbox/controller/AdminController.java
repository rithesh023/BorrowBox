package com.borrowbox.controller;

import com.borrowbox.dto.AdminSummary;
import com.borrowbox.service.AdminService;
import com.borrowbox.service.ItemService;
import com.borrowbox.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final ItemService itemService;
    public AdminController(AdminService adminService, ItemService itemService) { this.adminService = adminService; this.itemService = itemService; }
    @GetMapping("/summary") public AdminSummary summary() { return adminService.summary(); }
    @GetMapping("/users") public List<UserResponse> users() { return adminService.users(); }
    @GetMapping("/items") public List<ItemResponse> items() { return adminService.items(); }
    @GetMapping("/rentals") public List<RentalResponse> rentals() { return adminService.rentals(); }
    @GetMapping("/payments") public List<PaymentResponse> payments() { return adminService.payments(); }
    @PutMapping("/users/{id}/enabled") public UserResponse setUserEnabled(@PathVariable long id, @RequestParam boolean enabled, Authentication auth) {
        return adminService.setUserEnabled(id, enabled, auth.getName());
    }
    @PutMapping("/items/{id}/availability") public ItemResponse setItemAvailability(@PathVariable long id, @RequestParam boolean available) {
        return adminService.setItemAvailability(id, available);
    }
    @PutMapping("/items/{id}") public ItemResponse updateItem(@PathVariable long id, @Valid @RequestBody ItemRequest request) {
        return itemService.adminUpdate(id, request);
    }
    @DeleteMapping("/items/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable long id) { itemService.adminDelete(id); }
}
