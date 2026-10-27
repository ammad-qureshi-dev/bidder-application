package com.bidder.application.controllers.dtos.request.auth;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(String email, String phoneNumber, @NotNull String password) {}
