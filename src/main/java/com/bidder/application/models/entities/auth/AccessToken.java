/* (C) 2026
bidder.app */
package com.bidder.application.models.entities.auth;

import com.bidder.application.models.entities.BaseEntity;
import com.bidder.application.models.enums.auth.TokenType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "identity_and_auth_service", name = "access_token")
@EqualsAndHashCode(callSuper = true)
public class AccessToken extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne private AppUser appUser;

  @NotNull private String token;

  @NotNull private Instant expiresAt;

  private Instant usedAt;

  @Enumerated(EnumType.STRING)
  private TokenType tokenType;

  public boolean isExpired() {
    return Instant.now().isAfter(this.getExpiresAt());
  }
}
