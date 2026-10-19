package com.bidder.application.models.entities.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Metadata {
    private long viewCount;
    private long shareCount;
    private long favoriteCount;
}
