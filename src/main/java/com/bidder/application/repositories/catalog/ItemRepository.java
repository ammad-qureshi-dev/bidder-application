/* (C) 2026
bidder.app */
package com.bidder.application.repositories.catalog;

import com.bidder.application.models.entities.catalog.Item;
import com.bidder.application.utils.Constants;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, UUID> {

  // single atomic UPDATE: concurrent views don't lose increments, and @Version isn't
  // bumped (saving the entity would make in-flight bids fail with optimistic lock errors).
  // Clears the persistence context so the item can be re-read with the new count.
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "update "
              + Constants.Database.CATALOG_SCHEMA
              + ".item "
              + "set item_metadata = jsonb_set(coalesce(item_metadata, cast('{}' as jsonb)), '{viewCount}', "
              + "to_jsonb(coalesce(cast(item_metadata ->> 'viewCount' as bigint), 0) + 1)) "
              + "where id = :itemId",
      nativeQuery = true)
  void incrementViewCount(@Param("itemId") UUID itemId);
}
