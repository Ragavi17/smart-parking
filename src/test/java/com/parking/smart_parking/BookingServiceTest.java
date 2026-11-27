package com.parking.smart_parking;

import com.parking.smart_parking.dto.BookingRequest;
import com.parking.smart_parking.dto.BookingResponse;
import com.parking.smart_parking.entity.Booking;
import com.parking.smart_parking.entity.Slot;
import com.parking.smart_parking.entity.SlotType;
import com.parking.smart_parking.entity.User;
import com.parking.smart_parking.enums.BookingStatus;
import com.parking.smart_parking.enums.Role;
import com.parking.smart_parking.repository.BookingRepository;
import com.parking.smart_parking.repository.SlotRepository;
import com.parking.smart_parking.repository.UserRepository;
import com.parking.smart_parking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookingService bookingService;

    private Slot slot;
    private User arun;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        SlotType car = new SlotType();
        car.setId(1L);
        car.setName("CAR");
        car.setPricePerHour(new BigDecimal("40.00"));

        slot = new Slot();
        slot.setId(1L);
        slot.setSlotNumber("A1");
        slot.setActive(true);
        slot.setSlotType(car);

        arun = new User();
        arun.setId(1L);
        arun.setName("Arun Kumar");
        arun.setEmail("arun@test.com");
        arun.setRole(Role.USER);

        // always in the future, so the "start time cannot be in the past" rule never fails
        start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private void stubSuccessfulCreate(LocalDateTime end) {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsOverlap(eq(1L), eq(start), eq(end), anyCollection())).thenReturn(false);
        when(userRepository.findByEmail("arun@test.com")).thenReturn(Optional.of(arun));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking booking(User owner, BookingStatus status) {
        Booking b = new Booking();
        b.setId(5L);
        b.setUser(owner);
        b.setSlot(slot);
        b.setStartTime(start);
        b.setEndTime(start.plusHours(3));
        b.setPricePerHour(new BigDecimal("40.00"));
        b.setTotalAmount(new BigDecimal("120.00"));
        b.setStatus(status);
        return b;
    }

    @Test
    void create_success_returnsPendingBookingWithCorrectTotal() {
        LocalDateTime end = start.plusHours(3);
        stubSuccessfulCreate(end);

        BookingResponse response = bookingService.create("arun@test.com", new BookingRequest(1L, start, end));

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.pricePerHour()).isEqualByComparingTo("40.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("120.00");
        assertThat(response.slotNumber()).isEqualTo("A1");
    }

    @Test
    void create_roundsPartialHourUp() {
        LocalDateTime end = start.plusMinutes(90);
        stubSuccessfulCreate(end);

        BookingResponse response = bookingService.create("arun@test.com", new BookingRequest(1L, start, end));

        assertThat(response.totalAmount()).isEqualByComparingTo("80.00"); // 90 minutes billed as 2 hours
    }

    @Test
    void create_overlappingBooking_throwsConflictAndSavesNothing() {
        LocalDateTime end = start.plusHours(3);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsOverlap(eq(1L), eq(start), eq(end), anyCollection())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create("arun@test.com", new BookingRequest(1L, start, end)));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_inactiveSlot_throwsConflict() {
        slot.setActive(false);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create("arun@test.com", new BookingRequest(1L, start, start.plusHours(2))));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_endBeforeStart_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.create("arun@test.com", new BookingRequest(1L, start, start.minusHours(1))));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(slotRepository, bookingRepository);
    }

    @Test
    void cancel_byAnotherUser_throwsForbidden() {
        Booking booking = booking(arun, BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("someone.else@test.com", 5L));

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancel_byOwner_setsStatusCancelled() {
        Booking booking = booking(arun, BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancel("arun@test.com", 5L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_alreadyCancelledBooking_throwsBadRequest() {
        Booking booking = booking(arun, BookingStatus.CANCELLED);
        when(bookingRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancel("arun@test.com", 5L));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
    }
}