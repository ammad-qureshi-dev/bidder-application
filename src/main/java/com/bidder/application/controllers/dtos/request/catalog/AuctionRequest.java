package com.bidder.application.controllers.dtos.request.catalog;

import com.bidder.application.models.enums.catalog.AuctionCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record AuctionRequest(
    @NotNull String title,
    @NotNull Instant startTime,
    @NotNull Instant endTime,
    List<BiddingItemRequest> biddingItems,
    List<AuctionCategory> categories,
    @NotNull @Valid PickupLocationRequest pickupLocation) {}
