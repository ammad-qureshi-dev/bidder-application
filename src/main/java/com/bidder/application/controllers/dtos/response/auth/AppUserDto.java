package com.bidder.application.controllers.dtos.response.auth;

import java.util.UUID;

public record AppUserDto(UUID id, String fullName, PreferredContactMethod contact) {
}
