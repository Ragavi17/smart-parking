package com.parking.smart_parking.service;

import com.parking.smart_parking.dto.BookingExtensionRequest;
import com.parking.smart_parking.dto.ExtensionResponse;
import com.parking.smart_parking.entity.Booking;
import com.parking.smart_parking.entity.BookingExtension;
import com.parking.smart_parking.enums.BookingStatus;
import com.parking.smart_parking.enums.ExtensionStatus;
import com.parking.smart_parking.repository.BookingExtensionRepository;
import com.parking.smart_parking.repository.BookingRepository;
import com.parking.smart_parking.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtensionService {

    private static final List<BookingStatus> ACTIVE =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final long MAX_HOURS = 12;

    private final BookingRepository bookingRepository;
    private final BookingExtensionRepository extensionRepository;
    private final SlotRepository slotRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExtensionResponse request(String email, Long bookingId, BookingExtensionRequest request) {
        // 1. Lock the slot first, so concurrent bookings/extensions for this slot wait here
        Long slotId = bookingRepository.findSlotIdById(bookingId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Booking not found"));
        slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Slot not found"));

        // 2. Now read the booking (fresh data, because the slot lock is held)
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUser().getEmail().equals(email)) {
            throw fail(HttpStatus.FORBIDDEN, "This is not your booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw fail(HttpStatus.BAD_REQUEST, "Only confirmed bookings can be extended");
        }
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw fail(HttpStatus.BAD_REQUEST, "Booking has already ended");
        }

        LocalDateTime oldEnd = booking.getEndTime();
        LocalDateTime newEnd = request.newEndTime();
        long extraMinutes = Duration.between(oldEnd, newEnd).toMinutes();
        if (extraMinutes <= 0) {
            throw fail(HttpStatus.BAD_REQUEST, "New end time must be after the current end time");
        }
        if (extraMinutes % 60 != 0) {
            throw fail(HttpStatus.BAD_REQUEST, "Extension must be in whole hours");
        }
        if (Duration.between(booking.getStartTime(), newEnd).toMinutes() > MAX_HOURS * 60) {
            throw fail(HttpStatus.BAD_REQUEST, "A booking cannot be longer than " + MAX_HOURS + " hours");
        }
        if (extensionRepository.existsByBookingIdAndStatus(bookingId, ExtensionStatus.PENDING)) {
            throw fail(HttpStatus.CONFLICT, "An extension is already pending for this booking");
        }
        // Only the extra window is checked; this booking itself ends at oldEnd, so it never overlaps
        if (bookingRepository.existsOverlap(slotId, oldEnd, newEnd, ACTIVE)) {
            throw fail(HttpStatus.CONFLICT, "Slot is already booked for part of the extra time");
        }

        BookingExtension ext = new BookingExtension();
        ext.setBooking(booking);
        ext.setOldEndTime(oldEnd);
        ext.setNewEndTime(newEnd);
        ext.setExtraAmount(booking.getPricePerHour().multiply(BigDecimal.valueOf(extraMinutes / 60)));
        ext.setStatus(ExtensionStatus.PENDING);
        return toResponse(extensionRepository.save(ext));
    }

    // Temporary, until payment is built: the admin confirms the extension
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExtensionResponse confirm(Long extensionId) {
        Long slotId = extensionRepository.findSlotIdById(extensionId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Extension not found"));
        slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Slot not found"));

        BookingExtension ext = extensionRepository.findByIdWithBooking(extensionId)
                .orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Extension not found"));
        Booking booking = ext.getBooking();

        if (ext.getStatus() != ExtensionStatus.PENDING) {
            throw fail(HttpStatus.BAD_REQUEST, "Only pending extensions can be confirmed");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw fail(HttpStatus.BAD_REQUEST, "The booking is not confirmed");
        }
        if (!booking.getEndTime().equals(ext.getOldEndTime())) {
            throw fail(HttpStatus.CONFLICT, "The booking time has changed since this request");
        }
        if (bookingRepository.existsOverlap(slotId, ext.getOldEndTime(), ext.getNewEndTime(), ACTIVE)) {
            throw fail(HttpStatus.CONFLICT, "Slot is no longer free for the extra time");
        }

        booking.setEndTime(ext.getNewEndTime());
        booking.setTotalAmount(booking.getTotalAmount().add(ext.getExtraAmount()));
        ext.setStatus(ExtensionStatus.CONFIRMED);
        return toResponse(ext);
    }

    private ExtensionResponse toResponse(BookingExtension e) {
        return new ExtensionResponse(e.getId(), e.getBooking().getId(), e.getOldEndTime(),
                e.getNewEndTime(), e.getExtraAmount(), e.getStatus(), e.getCreatedAt());
    }

    private ResponseStatusException fail(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}