package com.borrowbox.dto;

import com.borrowbox.entity.AppUser;
import com.borrowbox.entity.Rental;

public record RentalContactsResponse(Contact owner, Contact borrower) {
    public static RentalContactsResponse from(Rental rental) {
        return new RentalContactsResponse(Contact.from(rental.getItem().getOwner()), Contact.from(rental.getBorrower()));
    }

    public record Contact(String name, String email, String phone) {
        private static Contact from(AppUser user) {
            return new Contact(user.getName(), user.getEmail(), user.getPhone());
        }
    }
}
