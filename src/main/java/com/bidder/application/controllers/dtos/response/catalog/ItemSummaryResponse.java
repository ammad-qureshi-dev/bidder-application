package com.bidder.application.controllers.dtos.response.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemSummaryResponse(
    UUID id,
    UUID auctionId,
    String title,
    String description,
    BigDecimal minimumPrice,
    boolean sold) {}
