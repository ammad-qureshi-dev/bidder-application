package com.bidder.application.models.entities.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionMetadata {
    private long viewCount;
    private long favoriteCount;
    private long shareCount;
}
