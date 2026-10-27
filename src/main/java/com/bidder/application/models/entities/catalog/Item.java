/* (C) 2026
bidder.app */
package com.bidder.application.models.entities.catalog;

import com.bidder.application.models.entities.BaseEntity;
import com.bidder.application.utils.Constants;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = Constants.Database.CATALOG_SCHEMA, name = "item")
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"auction"})
public class Item extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Entity relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "auction")
  private Auction auction;

  @NotNull
  @Length(max = 128)
  private String title;

  @Length(max = 512)
  private String description;

  @DecimalMin("0.0")
  private BigDecimal minimumPrice;

  @DecimalMin("0.0")
  private BigDecimal priceSoldAt;

  private UUID highestBidId;
  private BigDecimal highestBidAmount;

  @Builder.Default private boolean acceptingBids = true;

  @Valid
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "item_metadata", columnDefinition = "jsonb")
  private ItemMetadata itemMetadata;

  // For optimistic locking
  @Version private Long version;
}
