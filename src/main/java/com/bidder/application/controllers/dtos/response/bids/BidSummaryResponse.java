package com.bidder.application.controllers.dtos.response.bids;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.UUID;

import com.bidder.application.models.enums.bids.BidStatus;

public record BidSummaryResponse(UUID id, UUID itemId, UUID bidderId, BigDecimal amount, BidStatus status, String statusDescription,
								 Instant placedAt, Instant expiresAt) {
}
