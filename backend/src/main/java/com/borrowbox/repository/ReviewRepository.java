package com.borrowbox.repository;

import com.borrowbox.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByRentalId(Long rentalId);
    @Query("select r from Review r where r.rental.item.id = :itemId order by r.createdAt desc")
    List<Review> findForItem(@Param("itemId") Long itemId);
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.rental.item.id = :itemId")
    Double averageForItem(@Param("itemId") Long itemId);
    long countByRental_Item_Id(Long itemId);
}
