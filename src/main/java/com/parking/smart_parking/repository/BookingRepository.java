package com.parking.smart_parking.repository;

import com.parking.smart_parking.entity.Booking;
import com.parking.smart_parking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Two time ranges overlap when: existing.start < new.end AND existing.end > new.start
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.slot.id = :slotId
              and b.status in :statuses
              and b.startTime < :end
              and b.endTime > :start
            """)
    boolean existsOverlap(@Param("slotId") Long slotId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select b from Booking b
            join fetch b.slot s join fetch s.slotType
            where b.user.email = :email
            order by b.createdAt desc
            """)
    List<Booking> findByUserEmailWithDetails(@Param("email") String email);

    @Query("""
            select b from Booking b
            join fetch b.user join fetch b.slot s join fetch s.slotType
            where b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);
    @Query("select b.slot.id from Booking b where b.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);

    @Modifying
    @Query("""
            update Booking b set b.status = :newStatus, b.updatedAt = :now
            where b.status = :oldStatus and b.createdAt < :cutoff
            """)
    int expirePending(@Param("oldStatus") BookingStatus oldStatus,
                      @Param("newStatus") BookingStatus newStatus,
                      @Param("cutoff") LocalDateTime cutoff,
                      @Param("now") LocalDateTime now);
}