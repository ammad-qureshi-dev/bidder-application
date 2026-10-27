/* (C) 2026
bidder.app */
package com.bidder.application.scheduler.bids;

import com.bidder.application.configs.FeatureFlags;
import com.bidder.application.repositories.bids.BidRepository;
import com.bidder.application.services.bids.BidService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidScheduler {

  private final BidService bidService;
  private final BidRepository bidRepository;
  private final FeatureFlags featureFlags;

  @Transactional
  @Scheduled(fixedRate = 60000)
  public void recomputeHighestBidsAfterBidExpires() {

    if (!featureFlags.runRecomputeHighestBidsAfterBidExpires()) {
      log.info("RecomputeHighestBidsAfterBidExpires flag off");
      return;
    }

    log.info("BidScheduler: starting recomputeHighestBidsAfterBidExpires job...");

    var expiredBids = bidRepository.findItemIdsWithExpiredBids();

    if (expiredBids.isEmpty()) {
      log.info("No items found with expired bids @ {}", Instant.now());
      return;
    }

    expiredBids.forEach(bidService::processExpiredBid);

    log.info("BidScheduler: recomputeHighestBidsAfterBidExpires job completed");
  }
}
