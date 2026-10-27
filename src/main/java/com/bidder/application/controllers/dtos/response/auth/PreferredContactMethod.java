package com.bidder.application.controllers.dtos.response.auth;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import models.ContactType;

public record PreferredContactMethod(ContactType type, String value, @NotNull UUID appUserId) {}
