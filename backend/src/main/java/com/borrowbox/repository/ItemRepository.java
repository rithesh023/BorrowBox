package com.borrowbox.repository;

import com.borrowbox.entity.Item;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    @Query("select i from Item i where (:available is null or i.available = :available) " +
           "and (:category is null or lower(i.category.name) = lower(:category)) " +
           "and (:maxPrice is null or i.pricePerDay <= :maxPrice) " +
           "and (:condition is null or lower(i.itemCondition) = lower(:condition)) " +
           "and (:minRating is null or (select coalesce(avg(rv.rating), 0) from Review rv where rv.rental.item.id = i.id) >= :minRating) " +
           "and (:q is null or lower(i.name) like lower(concat('%',:q,'%')) or lower(i.description) like lower(concat('%',:q,'%'))) " +
           "and i.deleted = false")
    Page<Item> search(@Param("q") String q, @Param("category") String category,
                      @Param("maxPrice") java.math.BigDecimal maxPrice,
                      @Param("available") Boolean available, @Param("condition") String condition,
                      @Param("minRating") Double minRating, Pageable pageable);

    Page<Item> findByOwner_EmailIgnoreCaseAndDeletedFalse(String email, Pageable pageable);

    Page<Item> findByAvailableTrueAndDeletedFalse(Pageable pageable);

    Page<Item> findByCategory_NameIgnoreCaseAndAvailableTrueAndDeletedFalse(String category, Pageable pageable);

    Page<Item> findByOwner_EmailIgnoreCaseAndAvailableTrueAndDeletedFalse(String email, Pageable pageable);

    Page<Item> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();
}
