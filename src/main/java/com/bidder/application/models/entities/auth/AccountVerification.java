package com.bidder.application.models.entities.auth;

import com.bidder.application.models.enums.auth.AccountReminderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerification {
  private Instant lastRemindedOn;
  private Instant expirationDate;
  private boolean isVerified;

  @Enumerated(EnumType.STRING)
  private AccountReminderStatus status;

  private Instant verifiedOn;
}
