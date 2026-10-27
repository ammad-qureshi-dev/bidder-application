package com.bidder.application.controllers.dtos.response.catalog;

public record PickupLocationResponse(
    Long unit,
    String street,
    String city,
    String postalCode,
    String state,
    String country,
    String instructions) {}
