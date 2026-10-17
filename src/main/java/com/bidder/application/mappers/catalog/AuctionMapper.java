/* (C) 2026 
bidder.app */
package com.bidder.application.mappers.catalog;

import com.bidder.application.models.entities.catalog.Auction;
import com.bidder.application.controllers.dtos.request.catalog.AuctionRequest;
import com.bidder.application.controllers.dtos.response.catalog.AuctionResponse;
import com.bidder.application.controllers.dtos.response.catalog.AuctionSummaryResponse;

public class AuctionMapper {

	public static Auction requestToEntity(AuctionRequest request) {
		return Auction.builder().title(request.title()).startTime(request.startTime()).endTime(request.endTime())
				.items(request.biddingItems().stream().map(ItemMapper::requestToEntity).toList())
				.categories(request.categories())
				.pickupLocation(PickupLocationMapper.requestToEntity(request.pickupLocation())).build();
	}

	public static AuctionResponse entityToResponse(Auction entity) {
		var itemsDto = entity.getItems().stream().map(ItemMapper::entityToResponse).toList();
		return new AuctionResponse(entity.getId(), entity.getOwnerId(), entity.getTitle(), entity.getAuctionStatus(),
				entity.getCategories(), itemsDto, entity.getStartTime(), entity.getEndTime(),
				PickupLocationMapper.entityToResponse(entity.getPickupLocation()));
	}

	public static AuctionSummaryResponse entityToSummary(Auction a) {
		return new AuctionSummaryResponse(a.getId(), a.getTitle(), a.getAuctionStatus(), a.getCategories(),
				a.getItems().size(), a.getStartTime(), a.getEndTime(),
				PickupLocationMapper.entityToResponse(a.getPickupLocation()));
	}

}
