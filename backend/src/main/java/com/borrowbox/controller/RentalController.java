package com.borrowbox.controller;

import com.borrowbox.dto.*;
import com.borrowbox.service.RentalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {
    private final RentalService rentalService;
    public RentalController(RentalService rentalService) { this.rentalService = rentalService; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse request(Authentication auth, @Valid @RequestBody RentalRequest request) { return rentalService.request(auth.getName(), request); }
    @GetMapping("/my") public List<RentalResponse> mine(Authentication auth) { return rentalService.borrowerHistory(auth.getName()); }
    @GetMapping("/owner") public List<RentalResponse> ownerRequests(Authentication auth) { return rentalService.ownerRequests(auth.getName()); }
    @GetMapping("/{id}") public RentalResponse get(Authentication auth, @PathVariable long id) { return rentalService.get(auth.getName(), id); }
    @PutMapping("/{id}/accept") public RentalResponse accept(Authentication auth, @PathVariable long id) { return rentalService.accept(auth.getName(), id); }
    @PutMapping("/{id}/reject") public RentalResponse reject(Authentication auth, @PathVariable long id) { return rentalService.reject(auth.getName(), id); }
    @PostMapping("/{id}/pay") public PaymentResponse pay(Authentication auth, @PathVariable long id, @Valid @RequestBody PaymentRequest request) { return rentalService.pay(auth.getName(), id, request); }
    @PutMapping("/{id}/activate") public RentalResponse activate(Authentication auth, @PathVariable long id) { return rentalService.activate(auth.getName(), id); }
    @PutMapping("/{id}/complete") public RentalResponse complete(Authentication auth, @PathVariable long id) { return rentalService.complete(auth.getName(), id); }
    @PutMapping("/{id}/cancel") public RentalResponse cancel(Authentication auth, @PathVariable long id) { return rentalService.cancel(auth.getName(), id); }
}
