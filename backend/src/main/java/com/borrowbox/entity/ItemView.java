package com.borrowbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "item_views", indexes = @Index(name = "idx_item_view_user_created", columnList = "user_id,viewed_at"))
public class ItemView {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private AppUser user;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "item_id")
    private Item item;
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private Instant viewedAt = Instant.now();

    protected ItemView() {}
    public ItemView(AppUser user, Item item) { this.user = user; this.item = item; }
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Item getItem() { return item; }
    public Instant getViewedAt() { return viewedAt; }
}
