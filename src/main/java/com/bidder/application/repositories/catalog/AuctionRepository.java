/* (C) 2026 
bidder.app */
package com.bidder.application.repositories.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bidder.application.models.entities.catalog.Auction;
import com.bidder.application.models.enums.catalog.AuctionStatus;
import com.bidder.application.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

	// single atomic UPDATE so concurrent views don't lose increments (same as
	// ItemRepository.incrementViewCount). Clears the persistence context so the
	// auction can be re-read with the new count.
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "update " + Constants.Database.CATALOG_SCHEMA + ".auction "
			+ "set auction_metadata = jsonb_set(coalesce(auction_metadata, cast('{}' as jsonb)), '{viewCount}', "
			+ "to_jsonb(coalesce(cast(auction_metadata ->> 'viewCount' as bigint), 0) + 1)) "
			+ "where id = :auctionId", nativeQuery = true)
	void incrementViewCount(@Param("auctionId") UUID auctionId);

	@Query("""
			select A
			from Auction A
			where A.auctionStatus in ('LIVE', 'PAUSED')
			and A.endTime <= CURRENT_TIMESTAMP
			""")
	List<Auction> getExpiringAuctions();

	@Query("""
			select a
			from Auction a
			where (cast(:title as string) is null or lower(a.title) like concat('%', lower(cast(:title as string)), '%'))
			and (cast(:status as string) is null or a.auctionStatus = :status)
			and (cast(:startAfter as Instant) is null or a.startTime >= :startAfter)
			and (cast(:endBefore as Instant) is null or a.endTime <= :endBefore)
			""")
	Page<Auction> search(@Param("title") String title, @Param("status") AuctionStatus status,
			@Param("startAfter") Instant startAfter, @Param("endBefore") Instant endBefore, Pageable pageable);

	@Query("""
			select a
			from Auction a
			where a.ownerId = :appUserId
			""")
	Page<Auction> findMyAuctions(@Param("appUserId") UUID appUserId, Pageable pageable);

	@Query("""
			select A
			from Auction A
			where A.auctionStatus in ('UPCOMING')
			and A.startTime <= CURRENT_TIMESTAMP
			""")
	List<Auction> getUpcomingAuctions();

}
