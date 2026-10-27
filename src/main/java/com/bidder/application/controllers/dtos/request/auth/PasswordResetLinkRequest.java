package com.bidder.application.controllers.dtos.request.auth;

import jakarta.validation.constraints.NotNull;

public record PasswordResetLinkRequest(@NotNull String contact) {}
