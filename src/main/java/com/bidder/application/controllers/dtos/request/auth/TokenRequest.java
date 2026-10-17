package com.bidder.application.controllers.dtos.request.auth;

import models.ContactType;

public record TokenRequest(ContactType contactMethod, String email, String phoneNumber, String password) {

	@Override
	public String toString() {
		return """
				{
				    resetMethod: %s,
				    email: %s,
				    phoneNumber: %s,
				    password: ***
				}
				""".formatted(contactMethod, email, phoneNumber);
	}
}
