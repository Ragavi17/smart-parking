package com.parking.smart_parking.repository;

import com.parking.smart_parking.entity.BookingExtension;
import com.parking.smart_parking.enums.ExtensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingExtensionRepository extends JpaRepository<BookingExtension, Long> {

    boolean existsByBookingIdAndStatus(Long bookingId, ExtensionStatus status);

    @Query("select e.booking.slot.id from BookingExtension e where e.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);

    @Query("select e from BookingExtension e join fetch e.booking b join fetch b.slot where e.id = :id")
    Optional<BookingExtension> findByIdWithBooking(@Param("id") Long id);

    @Modifying
    @Query("""
            update BookingExtension e set e.status = :newStatus
            where e.status = :oldStatus and e.createdAt < :cutoff
            """)
    int expirePending(@Param("oldStatus") ExtensionStatus oldStatus,
                      @Param("newStatus") ExtensionStatus newStatus,
                      @Param("cutoff") LocalDateTime cutoff);
}