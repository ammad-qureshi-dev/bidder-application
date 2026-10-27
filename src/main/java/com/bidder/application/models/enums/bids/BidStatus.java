package com.bidder.application.models.enums.bids;

public enum BidStatus {
  WINNER, // Winner of the item
  REJECTED, // Rejected due to insufficient funds, cannot pay properly, etc.
  ACTIVE, // Current highest bid
  OUTBID,
  WITHDRAWN, // Bidder can opt to withdraw bid
  PENDING_VALIDATION,
  EXPIRED
}
