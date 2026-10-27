package com.bidder.application.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feature-flag")
public record FeatureFlags(
    boolean sendNotifications,
    boolean runRecomputeHighestBidsAfterBidExpires,
    boolean runCloseAuctionScheduleJob) {}
