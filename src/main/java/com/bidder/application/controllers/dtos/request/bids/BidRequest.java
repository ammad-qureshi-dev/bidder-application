package com.bidder.application.controllers.dtos.request.bids;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidRequest(
    @NotNull UUID auctionId,
    @NotNull UUID itemId,
    @NotNull BigDecimal amount,
    Instant expiresAt,
    String itemName) {}
