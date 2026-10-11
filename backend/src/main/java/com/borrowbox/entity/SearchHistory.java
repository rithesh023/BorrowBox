package com.borrowbox.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_history", indexes = @Index(name = "idx_search_user_created", columnList = "user_id,created_at"))
public class SearchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private AppUser user;
    @Column(name = "search_query", nullable = false, length = 250)
    private String query;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SearchHistory() {}
    public SearchHistory(AppUser user, String query) { this.user = user; this.query = query; }
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getQuery() { return query; }
    public Instant getCreatedAt() { return createdAt; }
}
