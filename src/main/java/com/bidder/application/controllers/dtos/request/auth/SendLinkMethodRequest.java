package com.bidder.application.controllers.dtos.request.auth;

import jakarta.validation.constraints.NotNull;
import models.ContactType;

public record SendLinkMethodRequest(@NotNull ContactType contactType) {}
