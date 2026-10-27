package com.bidder.application.controllers.dtos.response.catalog;

import com.bidder.application.models.enums.catalog.AuctionCategory;
import com.bidder.application.models.enums.catalog.AuctionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuctionSummaryResponse(
    UUID id,
    String title,
    AuctionStatus status,
    List<AuctionCategory> categories,
    int itemCount,
    Instant startTime,
    Instant endTime,
    PickupLocationResponse pickupLocation) {}
