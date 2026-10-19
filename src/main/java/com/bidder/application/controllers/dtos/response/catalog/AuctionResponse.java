package com.bidder.application.controllers.dtos.response.catalog;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bidder.application.models.enums.catalog.AuctionCategory;
import com.bidder.application.models.enums.catalog.AuctionStatus;

public record AuctionResponse(UUID id, UUID ownerId, String title, AuctionStatus auctionStatus,
		List<AuctionCategory> categories, List<ItemResponse> items, Instant startTime, Instant endTime,
		PickupLocationResponse pickupLocation, long viewCount) {
}
