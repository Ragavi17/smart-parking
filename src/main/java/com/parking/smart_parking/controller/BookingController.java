package com.parking.smart_parking.controller;

import com.parking.smart_parking.dto.BookingRequest;
import com.parking.smart_parking.dto.BookingResponse;
import com.parking.smart_parking.dto.SlotResponse;
import com.parking.smart_parking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/slots/available")
    public List<SlotResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Long slotTypeId) {
        return bookingService.findAvailable(start, end, slotTypeId);
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(authentication.getName(), request));
    }

    @GetMapping("/bookings/my")
    public List<BookingResponse> myBookings(Authentication authentication) {
        return bookingService.myBookings(authentication.getName());
    }

    @PatchMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Authentication authentication) {
        return bookingService.cancel(authentication.getName(), id);
    }
}