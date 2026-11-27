package com.parking.smart_parking.entity;

import com.parking.smart_parking.enums.ExtensionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_extensions")
@Getter @Setter @NoArgsConstructor
public class BookingExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "old_end_time", nullable = false)
    private LocalDateTime oldEndTime;

    @Column(name = "new_end_time", nullable = false)
    private LocalDateTime newEndTime;

    @Column(name = "extra_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtensionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}