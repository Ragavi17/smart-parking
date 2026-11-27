package com.parking.smart_parking.service;

import com.parking.smart_parking.dto.BookingRequest;
import com.parking.smart_parking.dto.BookingResponse;
import com.parking.smart_parking.dto.SlotResponse;
import com.parking.smart_parking.entity.Booking;
import com.parking.smart_parking.entity.Slot;
import com.parking.smart_parking.entity.SlotType;
import com.parking.smart_parking.entity.User;
import com.parking.smart_parking.enums.BookingStatus;
import com.parking.smart_parking.repository.BookingRepository;
import com.parking.smart_parking.repository.SlotRepository;
import com.parking.smart_parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final List<BookingStatus> ACTIVE =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final long MAX_HOURS = 12;

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SlotResponse> findAvailable(LocalDateTime start, LocalDateTime end, Long slotTypeId) {
        validateRange(start, end);
        return slotRepository.findAvailable(start, end, slotTypeId, ACTIVE).stream()
                .map(s -> {
                    SlotType t = s.getSlotType();
                    return new SlotResponse(s.getId(), s.getSlotNumber(), s.isActive(),
                            t.getId(), t.getName(), t.getPricePerHour());
                }).toList();
    }

    @Transactional
    public BookingResponse create(String email, BookingRequest request) {
        validateRange(request.startTime(), request.endTime());

        // IMPORTANT: lock the slot row FIRST, before any other read. A second request for
        // the same slot waits here until this transaction commits, then sees our booking.
        Slot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!slot.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is not active");
        }
        if (bookingRepository.existsOverlap(slot.getId(), request.startTime(), request.endTime(), ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is already booked for this time");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long minutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        long hours = (minutes + 59) / 60; // round up to whole hours
        BigDecimal price = slot.getSlotType().getPricePerHour();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setPricePerHour(price); // snapshot of today's price
        booking.setTotalAmount(price.multiply(BigDecimal.valueOf(hours)));
        booking.setStatus(BookingStatus.PENDING);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        return bookingRepository.findByUserEmailWithDetails(email).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancel(String email, Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your booking");
        }
        if (!ACTIVE.contains(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending or confirmed bookings can be cancelled");
        }
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has already ended");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(booking);
    }

    // Temporary, until payment is built: lets the admin confirm a pending booking
    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(booking);
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time cannot be in the past");
        }
        if (Duration.between(start, end).toMinutes() > MAX_HOURS * 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A booking cannot be longer than " + MAX_HOURS + " hours");
        }
    }

    private BookingResponse toResponse(Booking b) {
        Slot s = b.getSlot();
        return new BookingResponse(b.getId(), s.getId(), s.getSlotNumber(),
                s.getSlotType().getName(), b.getStartTime(), b.getEndTime(),
                b.getPricePerHour(), b.getTotalAmount(), b.getStatus(), b.getCreatedAt());
    }
}