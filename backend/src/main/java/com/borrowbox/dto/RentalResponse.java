package com.borrowbox.dto;

import com.borrowbox.entity.Rental;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentalResponse(Long id, Long itemId, String itemName, boolean itemDeleted, Long ownerId, String ownerName,
                             Long borrowerId, String borrowerName, LocalDate startDate, LocalDate endDate,
                             BigDecimal rentalAmount, BigDecimal securityDeposit, BigDecimal totalAmount,
                             String status, Instant createdAt, RentalContactsResponse pickupContacts) {
    public static RentalResponse from(Rental rental) {
        return from(rental, null);
    }

    public static RentalResponse fromParticipant(Rental rental) {
        boolean contactsAvailable = switch (rental.getStatus()) {
            case ACCEPTED, PAID, ACTIVE -> true;
            default -> false;
        };
        return from(rental, contactsAvailable ? RentalContactsResponse.from(rental) : null);
    }

    private static RentalResponse from(Rental rental, RentalContactsResponse pickupContacts) {
        return new RentalResponse(rental.getId(), rental.getItem().getId(), rental.getItem().getName(), rental.getItem().isDeleted(),
                rental.getItem().getOwner().getId(), rental.getItem().getOwner().getName(),
                rental.getBorrower().getId(), rental.getBorrower().getName(), rental.getStartDate(),
                rental.getEndDate(), rental.getRentalAmount(), rental.getSecurityDeposit(), rental.getTotalAmount(),
                rental.getStatus().name(), rental.getCreatedAt(), pickupContacts);
    }
}
