package com.bidder.application.controllers.dtos.response.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponse(
    UUID id,
    String title,
    String description,
    BigDecimal minimumPrice,
    BigDecimal priceSoldAt,
    UUID highestBidId,
    BigDecimal highestBidAmount,
    boolean acceptingBids,
    long viewCount) {}
