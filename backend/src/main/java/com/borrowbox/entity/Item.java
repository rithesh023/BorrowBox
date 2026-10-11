package com.borrowbox.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "items", indexes = @Index(name = "idx_item_available", columnList = "available"))
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "owner_id")
    private AppUser owner;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, length = 140)
    private String name;
    @Column(nullable = false, length = 3000)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;
    @Column(nullable = false, length = 40)
    private String itemCondition;
    @Column(length = 1000)
    private String imageUrl;
    @Column(nullable = false)
    private boolean available = true;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Item() {}
    public Item(AppUser owner, Category category, String name, String description, BigDecimal pricePerDay,
                BigDecimal securityDeposit, String itemCondition, String imageUrl) {
        this.owner = owner; this.category = category; this.name = name; this.description = description;
        this.pricePerDay = pricePerDay; this.securityDeposit = securityDeposit;
        this.itemCondition = itemCondition; this.imageUrl = imageUrl;
    }
    public void update(Category category, String name, String description, BigDecimal pricePerDay,
                       BigDecimal securityDeposit, String itemCondition, String imageUrl, boolean available) {
        this.category = category; this.name = name; this.description = description; this.pricePerDay = pricePerDay;
        this.securityDeposit = securityDeposit; this.itemCondition = itemCondition; this.imageUrl = imageUrl;
        this.available = available;
    }
    public void setAvailable(boolean available) { this.available = available; }
    public void markDeleted() { this.deleted = true; this.available = false; }
    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public BigDecimal getSecurityDeposit() { return securityDeposit; }
    public String getItemCondition() { return itemCondition; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }
}
