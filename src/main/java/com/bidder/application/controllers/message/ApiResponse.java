/* (C) 2026
bidder.app */
package com.bidder.application.controllers.message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jdk.jfr.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
  private T data;

  @Builder.Default @Timestamp private LocalDateTime completedAt = LocalDateTime.now();

  @Builder.Default private UUID requestId = UUID.randomUUID();

  @Builder.Default private List<ApiMessage> messages = new ArrayList<>();
}
