/* (C) 2026 
bidder.app */
package com.bidder.application.services.bids;

import java.util.UUID;

import com.bidder.application.repositories.bids.BidRepository;
import lombok.RequiredArgsConstructor;
import com.bidder.application.models.entities.bids.Bid;
import com.bidder.application.services.catalog.AuctionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidValidation {
	private final BidRepository bidRepository;
	private final AuctionService auctionService;

	public void rejectBidValidation(Bid bid, UUID ownerId) throws IllegalAccessException {
		if (bid == null) {
			throw new NullPointerException("Null bid was provided");
		}

		if (bid.getBidderId().equals(ownerId)) {
			throw new IllegalAccessException("Owner cannot reject their own bid");
		}

		final var auction = auctionService.getAuctionById(bid.getAuctionId());
		if (!auction.getOwnerId().equals(ownerId)) {
			throw new IllegalAccessException("Cannot reject this bid as you don't own this bid");
		}
	}
}
