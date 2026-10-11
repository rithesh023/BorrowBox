package com.borrowbox.service;

import com.borrowbox.dto.*;
import com.borrowbox.entity.*;
import com.borrowbox.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RentalService {
    private static final List<RentalStatus> BLOCKING = List.of(RentalStatus.ACCEPTED, RentalStatus.PAID, RentalStatus.ACTIVE);
    private final RentalRepository rentals;
    private final ItemRepository items;
    private final UserRepository users;
    private final PaymentRepository payments;
    public RentalService(RentalRepository rentals, ItemRepository items, UserRepository users, PaymentRepository payments) {
        this.rentals = rentals; this.items = items; this.users = users; this.payments = payments;
    }

    @Transactional
    public RentalResponse request(String email, RentalRequest request) {
        if (request.startDate().isBefore(LocalDate.now()) || request.endDate().isBefore(request.startDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rental dates must be valid and cannot start in the past");
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (days > 90) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rental cannot exceed 90 days");
        Item item = items.findByIdForUpdate(request.itemId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (!item.isAvailable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Item is unavailable");
        if (item.getOwner().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot rent your own item");
        if (rentals.hasConflict(item.getId(), request.startDate(), request.endDate(), -1L, BLOCKING))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already has a confirmed rental during those dates");
        AppUser borrower = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        BigDecimal fee = item.getPricePerDay().multiply(BigDecimal.valueOf(days));
        Rental rental = new Rental(item, borrower, request.startDate(), request.endDate(), fee,
                item.getSecurityDeposit(), fee.add(item.getSecurityDeposit()));
        return RentalResponse.fromParticipant(rentals.save(rental));
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> borrowerHistory(String email) {
        return rentals.findByBorrower_EmailIgnoreCaseOrderByCreatedAtDesc(email).stream().map(RentalResponse::fromParticipant).toList();
    }
    @Transactional(readOnly = true)
    public List<RentalResponse> ownerRequests(String email) {
        return rentals.findByItem_Owner_EmailIgnoreCaseOrderByCreatedAtDesc(email).stream().map(RentalResponse::fromParticipant).toList();
    }
    @Transactional(readOnly = true)
    public RentalResponse get(String email, long id) {
        Rental rental = findRental(id); requireParticipant(email, rental); return RentalResponse.fromParticipant(rental);
    }

    @Transactional
    public RentalResponse accept(String email, long id) {
        Rental rental = findRentalForUpdate(id); requireOwner(email, rental);
        if (rental.getStatus() != RentalStatus.PENDING) throw invalidTransition();
        items.findByIdForUpdate(rental.getItem().getId()).orElseThrow();
        if (!rental.getItem().isAvailable() || rentals.hasConflict(rental.getItem().getId(), rental.getStartDate(), rental.getEndDate(), id, BLOCKING))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Those rental dates are no longer available");
        rental.setStatus(RentalStatus.ACCEPTED);
        return RentalResponse.fromParticipant(rental);
    }
    @Transactional
    public RentalResponse reject(String email, long id) {
        Rental rental = findRentalForUpdate(id); requireOwner(email, rental);
        if (rental.getStatus() != RentalStatus.PENDING) throw invalidTransition();
        rental.setStatus(RentalStatus.REJECTED); return RentalResponse.fromParticipant(rental);
    }
    @Transactional
    public PaymentResponse pay(String email, long id, PaymentRequest request) {
        Rental rental = findRentalForUpdate(id); requireBorrower(email, rental);
        if (rental.getStatus() != RentalStatus.ACCEPTED) throw invalidTransition();
        Payment payment = payments.save(new Payment(rental, rental.getTotalAmount(), request.method()));
        rental.setStatus(RentalStatus.PAID);
        return PaymentResponse.from(payment);
    }
    @Transactional
    public RentalResponse activate(String email, long id) {
        Rental rental = findRentalForUpdate(id); requireOwner(email, rental);
        if (rental.getStatus() != RentalStatus.PAID) throw invalidTransition();
        rental.setStatus(RentalStatus.ACTIVE); return RentalResponse.fromParticipant(rental);
    }
    @Transactional
    public RentalResponse complete(String email, long id) {
        Rental rental = findRentalForUpdate(id); requireParticipant(email, rental);
        if (rental.getStatus() != RentalStatus.ACTIVE) throw invalidTransition();
        rental.setStatus(RentalStatus.COMPLETED); return RentalResponse.fromParticipant(rental);
    }
    @Transactional
    public RentalResponse cancel(String email, long id) {
        Rental rental = findRentalForUpdate(id); requireBorrower(email, rental);
        if (rental.getStatus() != RentalStatus.PENDING && rental.getStatus() != RentalStatus.ACCEPTED)
            throw invalidTransition();
        rental.setStatus(RentalStatus.CANCELLED); return RentalResponse.fromParticipant(rental);
    }

    private Rental findRental(long id) { return rentals.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found")); }
    private Rental findRentalForUpdate(long id) { return rentals.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found")); }
    private void requireOwner(String email, Rental rental) {
        if (!rental.getItem().getOwner().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the item owner can perform this action");
    }
    private void requireBorrower(String email, Rental rental) {
        if (!rental.getBorrower().getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the borrower can perform this action");
    }
    private void requireParticipant(String email, Rental rental) {
        if (!rental.getBorrower().getEmail().equalsIgnoreCase(email) && !rental.getItem().getOwner().getEmail().equalsIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this rental");
    }
    private ResponseStatusException invalidTransition() { return new ResponseStatusException(HttpStatus.CONFLICT, "Rental status does not allow this action"); }
}
