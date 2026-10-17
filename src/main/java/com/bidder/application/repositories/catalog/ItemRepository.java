/* (C) 2026 
bidder.app */
package com.bidder.application.repositories.catalog;

import java.util.UUID;

import com.bidder.application.models.entities.catalog.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

}
