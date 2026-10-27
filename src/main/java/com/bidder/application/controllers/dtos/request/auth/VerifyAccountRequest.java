package com.bidder.application.controllers.dtos.request.auth;

import jakarta.validation.constraints.NotNull;

public record VerifyAccountRequest(@NotNull String token) {}
