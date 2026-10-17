/* (C) 2026 
bidder.app */
package com.bidder.application.mappers.catalog;

import com.bidder.application.models.entities.catalog.Item;
import com.bidder.application.controllers.dtos.request.catalog.BiddingItemRequest;
import com.bidder.application.controllers.dtos.response.catalog.ItemResponse;
import com.bidder.application.controllers.dtos.response.catalog.ItemSummaryResponse;

public class ItemMapper {

	public static Item requestToEntity(BiddingItemRequest request) {
		return Item.builder().title(request.title()).description(request.description())
				.minimumPrice(request.minimumPrice()).build();
	}

	public static ItemResponse entityToResponse(Item entity) {

		if (entity == null) {
			return null;
		}

		return new ItemResponse(entity.getId(), entity.getTitle(), entity.getDescription(), entity.getMinimumPrice(),
				entity.getPriceSoldAt(), entity.getHighestBidId(), entity.getHighestBidAmount(),
				entity.isAcceptingBids());
	}

	public static ItemSummaryResponse entityToSummary(Item i) {
		if (i == null) {
			return null;
		}

		var sold = i.getPriceSoldAt() != null;
		return new ItemSummaryResponse(i.getId(), i.getAuction().getId(), i.getTitle(), i.getDescription(),
				i.getMinimumPrice(), sold);
	}
}
