package com.bidder.application.controllers.dtos.response.auth;

import java.util.UUID;

public record AuthResponse(String token, UUID userId) {
}
