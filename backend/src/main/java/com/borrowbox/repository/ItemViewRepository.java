package com.borrowbox.repository;

import com.borrowbox.entity.ItemView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemViewRepository extends JpaRepository<ItemView, Long> {
    List<ItemView> findTop50ByUser_IdOrderByViewedAtDesc(Long userId);
    long countByItem_Id(Long itemId);
}
