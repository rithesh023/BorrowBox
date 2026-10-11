package com.borrowbox.service;

import com.borrowbox.entity.*;
import com.borrowbox.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final UserRepository users;
    private final SearchHistoryRepository searches;
    private final ItemViewRepository views;
    private final ItemRepository items;

    public ActivityService(UserRepository users, SearchHistoryRepository searches, ItemViewRepository views, ItemRepository items) {
        this.users = users; this.searches = searches; this.views = views; this.items = items;
    }

    @Transactional
    public void recordSearch(String email, String query) {
        if (query == null || query.isBlank()) return;
        users.findByEmail(email).ifPresent(user -> searches.save(new SearchHistory(user, query.trim().substring(0, Math.min(250, query.trim().length())))));
    }

    @Transactional
    public void recordView(String email, long itemId) {
        if (email == null) return;
        var user = users.findByEmail(email);
        var item = items.findById(itemId);
        if (user.isPresent() && item.isPresent()) views.save(new ItemView(user.get(), item.get()));
    }
}
