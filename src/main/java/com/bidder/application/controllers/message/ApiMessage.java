/* (C) 2026 
bidder.app */
package com.bidder.application.controllers.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiMessage {
	private ResponseType type;
	private String content;
}
