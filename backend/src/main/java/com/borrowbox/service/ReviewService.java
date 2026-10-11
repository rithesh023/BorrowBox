package com.borrowbox.service;

import com.borrowbox.dto.*;
import com.borrowbox.entity.*;
import com.borrowbox.repository.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final RentalRepository rentals;
    private final UserRepository users;
    public ReviewService(ReviewRepository reviews, RentalRepository rentals, UserRepository users) {
        this.reviews = reviews; this.rentals = rentals; this.users = users;
    }
    @Transactional
    public ReviewResponse create(String email, ReviewRequest request) {
        Rental rental = rentals.findByIdForUpdate(request.rentalId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found"));
        if (!rental.getBorrower().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the borrower can review this rental");
        if (rental.getStatus() != RentalStatus.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviews are available after a rental is completed");
        if (reviews.existsByRentalId(rental.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This rental already has a review");
        AppUser reviewer = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        return ReviewResponse.from(reviews.save(new Review(rental, reviewer, request.rating(), request.comment())));
    }
    @Transactional(readOnly = true)
    public List<ReviewResponse> byItem(long itemId) { return reviews.findForItem(itemId).stream().map(ReviewResponse::from).toList(); }

    @Transactional(readOnly = true)
    public boolean hasReviewed(String email, long rentalId) {
        Rental rental = rentals.findById(rentalId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found"));
        if (!rental.getBorrower().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the borrower can review this rental");
        return reviews.existsByRentalId(rentalId);
    }
}
