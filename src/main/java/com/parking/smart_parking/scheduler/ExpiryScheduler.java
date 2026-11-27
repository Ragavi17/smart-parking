package com.parking.smart_parking.scheduler;

import com.parking.smart_parking.enums.BookingStatus;
import com.parking.smart_parking.enums.ExtensionStatus;
import com.parking.smart_parking.repository.BookingExtensionRepository;
import com.parking.smart_parking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingExtensionRepository extensionRepository;

    @Value("${app.booking.pending-minutes:10}")
    private long pendingMinutes;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireUnpaid() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(pendingMinutes);
        int bookings = bookingRepository.expirePending(
                BookingStatus.PENDING, BookingStatus.EXPIRED, cutoff, now);
        int extensions = extensionRepository.expirePending(
                ExtensionStatus.PENDING, ExtensionStatus.EXPIRED, cutoff);
        if (bookings > 0 || extensions > 0) {
            log.info("Expired {} unpaid booking(s) and {} unpaid extension(s)", bookings, extensions);
        }
    }
}