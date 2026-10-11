package com.borrowbox.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    protected Category() {}
    public Category(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}
