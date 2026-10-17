package com.bidder.application.controllers.dtos.response.auth;

import jakarta.validation.constraints.NotNull;
import models.ContactType;

import java.util.UUID;

public record PreferredContactMethod(
        ContactType type,
        String value,
        @NotNull UUID appUserId
) {
}
