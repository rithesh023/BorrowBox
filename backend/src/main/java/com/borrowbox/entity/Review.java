package com.borrowbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_rental", columnNames = "rental_id"))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "rental_id", unique = true)
    private Rental rental;
    @ManyToOne(optional = false) @JoinColumn(name = "reviewer_id")
    private AppUser reviewer;
    @Column(nullable = false)
    private int rating;
    @Column(length = 1500)
    private String comment;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Review() {}
    public Review(Rental rental, AppUser reviewer, int rating, String comment) {
        this.rental = rental; this.reviewer = reviewer; this.rating = rating; this.comment = comment;
    }
    public Long getId() { return id; }
    public Rental getRental() { return rental; }
    public AppUser getReviewer() { return reviewer; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
