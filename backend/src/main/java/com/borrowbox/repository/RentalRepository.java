package com.borrowbox.repository;

import com.borrowbox.entity.Rental;
import com.borrowbox.entity.RentalStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rental r where r.id = :id")
    Optional<Rental> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(r) > 0 from Rental r where r.item.id = :itemId and r.id <> :excludeId " +
           "and r.status in :statuses and r.startDate <= :endDate and r.endDate >= :startDate")
    boolean hasConflict(@Param("itemId") Long itemId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, @Param("excludeId") Long excludeId,
                        @Param("statuses") List<RentalStatus> statuses);

    List<Rental> findByBorrower_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Rental> findByItem_Owner_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    long countByStatus(RentalStatus status);
    boolean existsByItem_Id(Long itemId);
}
