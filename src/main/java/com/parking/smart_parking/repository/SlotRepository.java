package com.parking.smart_parking.repository;

import com.parking.smart_parking.entity.Slot;
import com.parking.smart_parking.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    boolean existsBySlotNumberIgnoreCase(String slotNumber);

    @Query("select s from Slot s join fetch s.slotType order by s.slotNumber")
    List<Slot> findAllWithType();

    // Locks the slot row until the transaction ends (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s from Slot s join fetch s.slotType t
            where s.active = true
              and (:typeId is null or t.id = :typeId)
              and not exists (
                  select 1 from Booking b
                  where b.slot = s
                    and b.status in :statuses
                    and b.startTime < :end
                    and b.endTime > :start)
            order by s.slotNumber
            """)
    List<Slot> findAvailable(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             @Param("typeId") Long typeId,
                             @Param("statuses") Collection<BookingStatus> statuses);
}