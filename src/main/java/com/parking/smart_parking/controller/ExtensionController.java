package com.parking.smart_parking.controller;

import com.parking.smart_parking.dto.BookingExtensionRequest;
import com.parking.smart_parking.dto.ExtensionResponse;
import com.parking.smart_parking.service.ExtensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExtensionController {

    private final ExtensionService extensionService;

    @PostMapping("/bookings/{id}/extensions")
    public ResponseEntity<ExtensionResponse> request(@PathVariable Long id,
                                                     @Valid @RequestBody BookingExtensionRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extensionService.request(authentication.getName(), id, request));
    }

    @PostMapping("/admin/extensions/{id}/confirm")
    public ExtensionResponse confirm(@PathVariable Long id) {
        return extensionService.confirm(id);
    }
}