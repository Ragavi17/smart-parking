package com.parking.smart_parking.dto;

public record AuthResponse(String token, String name, String email, String role) {}