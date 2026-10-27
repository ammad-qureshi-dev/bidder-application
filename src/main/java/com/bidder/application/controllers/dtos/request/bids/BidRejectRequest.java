package com.bidder.application.controllers.dtos.request.bids;

import jakarta.validation.constraints.NotNull;

public record BidRejectRequest(@NotNull String itemName, @NotNull String rejectReason) {}
