package com.borrowbox.repository;

import com.borrowbox.entity.SearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);
}
