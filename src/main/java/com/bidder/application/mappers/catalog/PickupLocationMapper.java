/* (C) 2026 
bidder.app */
package com.bidder.application.mappers.catalog;

import com.bidder.application.models.entities.catalog.PickupLocation;
import com.bidder.application.controllers.dtos.request.catalog.PickupLocationRequest;
import com.bidder.application.controllers.dtos.response.catalog.PickupLocationResponse;

public class PickupLocationMapper {

	public static PickupLocation requestToEntity(PickupLocationRequest request) {
		if (request == null) {
			return null;
		}

		return PickupLocation.builder().unit(request.unit()).street(request.street()).city(request.city())
				.postalCode(request.postalCode()).state(request.state()).country(request.country())
				.instructions(request.instructions()).build();
	}

	public static PickupLocationResponse entityToResponse(PickupLocation entity) {
		if (entity == null) {
			return null;
		}

		return new PickupLocationResponse(entity.getUnit(), entity.getStreet(), entity.getCity(),
				entity.getPostalCode(), entity.getState(), entity.getCountry(), entity.getInstructions());
	}
}
