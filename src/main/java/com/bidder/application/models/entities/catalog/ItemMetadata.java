package com.bidder.application.models.entities.catalog;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// no fields of its own, so @SuperBuilder/@NoArgsConstructor only (an @AllArgsConstructor would
// clash with the no-arg one)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class ItemMetadata extends Metadata {}
