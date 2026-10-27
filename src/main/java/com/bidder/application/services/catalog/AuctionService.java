/* (C) 2026
bidder.app */
package com.bidder.application.services.catalog;

import static com.bidder.application.utils.Constants.AUCTION_URL;

import com.bidder.application.configs.kafka.EventTopics;
import com.bidder.application.controllers.dtos.request.catalog.AuctionRequest;
import com.bidder.application.controllers.dtos.request.catalog.PickupLocationRequest;
import com.bidder.application.controllers.dtos.response.bids.BidSummaryResponse;
import com.bidder.application.controllers.dtos.response.catalog.AuctionResponse;
import com.bidder.application.controllers.dtos.response.catalog.AuctionSummaryResponse;
import com.bidder.application.controllers.dtos.response.catalog.ItemSummaryResponse;
import com.bidder.application.controllers.dtos.response.catalog.PageResponse;
import com.bidder.application.mappers.catalog.AuctionMapper;
import com.bidder.application.mappers.catalog.ItemMapper;
import com.bidder.application.mappers.catalog.PickupLocationMapper;
import com.bidder.application.models.entities.catalog.Auction;
import com.bidder.application.models.entities.catalog.Item;
import com.bidder.application.models.enums.bids.BidStatus;
import com.bidder.application.models.enums.catalog.AuctionStatus;
import com.bidder.application.repositories.catalog.AuctionRepository;
import com.bidder.application.services.bids.BidService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.TemplateName;
import models.dtos.request.NotifyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

  @Value("${client-url}")
  private String clientUrl;

  private final AuctionRepository auctionRepository;
  // lazy: BidService also depends on AuctionService
  @Lazy private final BidService bidService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public UUID createAuction(AuctionRequest request, UUID appUserId) {
    var isFutureDated = isFutureDated(request.startTime());

    validationAuctionRequest(request);

    final var auction = AuctionMapper.requestToEntity(request);
    auction.setOwnerId(appUserId);
    auction.getItems().forEach(i -> i.setAuction(auction));

    auction.setAuctionStatus(isFutureDated ? AuctionStatus.UPCOMING : AuctionStatus.LIVE);

    // items are persisted via cascade
    auctionRepository.save(auction);

    return auction.getId();
  }

  public UUID updateAuction(UUID auctionId, AuctionRequest request) {
    validationAuctionRequest(request);

    final var auction = auctionRepository.findById(auctionId).orElseThrow();
    auction.setTitle(request.title());
    auction.setStartTime(request.startTime());
    auction.setEndTime(request.endTime());
    auction.setCategories(request.categories());
    auction.setPickupLocation(PickupLocationMapper.requestToEntity(request.pickupLocation()));

    final var newItems =
        request.biddingItems().stream().filter(e -> e.id() == null).toList().stream()
            .map(ItemMapper::requestToEntity)
            .toList();

    newItems.forEach(i -> i.setAuction(auction));

    auction.getItems().addAll(newItems);

    auctionRepository.save(auction);

    return auction.getId();
  }

  public Auction getAuctionById(UUID id) {
    return auctionRepository
        .findById(id)
        .orElseThrow(() -> new IllegalStateException("Auction not found"));
  }

  /** Returns the auction without recording a view. */
  public AuctionResponse getAuctionResponse(UUID auctionId) {
    return AuctionMapper.entityToResponse(getAuctionById(auctionId));
  }

  /**
   * Returns the auction and records a view, unless the viewer is the auction owner. Anonymous
   * viewers (viewerId == null) are counted.
   */
  @Transactional
  public AuctionResponse viewAuction(UUID auctionId, UUID viewerId) {
    var auction = getAuctionById(auctionId);

    if (!auction.getOwnerId().equals(viewerId)) {
      auctionRepository.incrementViewCount(auctionId);
      // re-read so the response includes this view
      auction = getAuctionById(auctionId);
    }

    return AuctionMapper.entityToResponse(auction);
  }

  public PageResponse<AuctionSummaryResponse> searchAuctions(
      String title,
      AuctionStatus status,
      Instant startAfter,
      Instant endBefore,
      Pageable pageable) {
    var auctions =
        auctionRepository
            .search(title, status, startAfter, endBefore, pageable)
            .map(AuctionMapper::entityToSummary);
    return PageResponse.from(auctions);
  }

  private static boolean isAuctionOpen(final Auction auction) {
    final var currStatus = auction.getAuctionStatus();
    return !(AuctionStatus.CLOSED.equals(currStatus)
        || Instant.now().isAfter(auction.getEndTime()));
  }

  @Transactional
  public void updateAuctionStatus(UUID auctionId, AuctionStatus status) {
    final var auction = getAuctionById(auctionId);
    final var currStatus = auction.getAuctionStatus();

    if (currStatus.equals(status)) {
      return;
    }

    if (status.equals(AuctionStatus.LIVE) || status.equals(AuctionStatus.PAUSED)) {
      if (!isAuctionOpen(auction)) {
        throw new IllegalStateException(
            "Auction is " + AuctionStatus.CLOSED + ", cannot perform update");
      }

      auction.setAuctionStatus(status);
      auctionRepository.save(auction);
    } else if (status.equals(AuctionStatus.CLOSED)) {
      if (!isAuctionOpen(auction)) {
        // no need to throw exception
        return;
      }

      auction.setAuctionStatus(status);
      auction.setAuctionClosedOn(Instant.now());
      auctionRepository.save(auction);
      processCloseAuction(auction);
    }

    notifyAuctionStatusToOwner(auction);
  }

  public PageResponse<AuctionSummaryResponse> getMyAuctions(UUID appUserId, Pageable pageable) {
    var auctions =
        auctionRepository.findMyAuctions(appUserId, pageable).map(AuctionMapper::entityToSummary);
    return PageResponse.from(auctions);
  }

  public List<ItemSummaryResponse> getItemsInAuction(UUID auctionId) {
    var auction = getAuctionById(auctionId);
    var items = auction.getItems();

    if (items == null || items.isEmpty()) {
      return List.of();
    }

    return items.stream().map(ItemMapper::entityToSummary).toList();
  }

  /*
   * test: > close auction > ammadq3 - auctioner owner > ammadq87 - highest bidder
   * > ammadq87 should receive email for bid accepted > ammadq3 should receive
   * email for auction closed
   */

  public void processCloseAuction(Auction auction) {
    // items with no bids have a null highestBidId
    var itemsWithBids =
        auction.getItems().stream().filter(i -> i.getHighestBidId() != null).toList();
    if (itemsWithBids.isEmpty()) {
      notifyAuctionStatusToOwner(auction);
      return;
    }

    var bids =
        bidService.getBids(itemsWithBids.stream().map(Item::getHighestBidId).toList()).getData();

    // id <-> bid
    var bidsById =
        bids.stream().collect(Collectors.toMap(BidSummaryResponse::id, b -> b, (a, b) -> a));

    bidService.internalBulkUpdateBidsStatuses(
        bidsById.keySet().stream().toList(), BidStatus.WINNER);

    // per bidder Id send message
    for (var item : itemsWithBids) {
      var bid = bidsById.get(item.getHighestBidId());
      if (bid == null) {
        log.warn("Bid {} for item {} not found", item.getHighestBidId(), item.getId());
        continue;
      }

      var auctionUrl = generateAuctionUrl(item.getAuction().getId());

      kafkaTemplate.send(
          EventTopics.NOTIFICATION.getTopic(),
          new NotifyRequest(
              bid.bidderId(),
              TemplateName.BID_REQUEST_ACCEPTED,
              null,
              Map.of(
                  "itemName",
                  item.getTitle(),
                  "bidAmount",
                  bid.amount(),
                  AUCTION_URL,
                  auctionUrl)));
    }
  }

  public void notifyAuctionStatusToOwner(Auction auction) {
    var templateData = new HashMap<String, Object>();
    TemplateName templateName = null;

    var currStatus = auction.getAuctionStatus();
    var auctionUrl = generateAuctionUrl(auction.getId());

    switch (currStatus) {
      case AuctionStatus.CLOSED -> {
        templateName = TemplateName.AUCTION_CLOSED;
        templateData =
            new HashMap<>(
                Map.of(
                    AUCTION_URL,
                    auctionUrl,
                    "items",
                    auction.getItems().stream().map(ItemMapper::entityToResponse).toList()));
      }

      case AuctionStatus.LIVE -> {
        templateName = TemplateName.AUCTION_LIVE;
        templateData = new HashMap<>(Map.of(AUCTION_URL, auctionUrl, "title", auction.getTitle()));
      }

      case AuctionStatus.PAUSED -> {
        templateName = TemplateName.AUCTION_PAUSED;
        templateData = new HashMap<>(Map.of(AUCTION_URL, auctionUrl, "title", auction.getTitle()));
      }
    }

    var notifyRequest = new NotifyRequest(auction.getOwnerId(), templateName, null, templateData);

    kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(), notifyRequest);
  }

  private static void validationAuctionRequest(AuctionRequest request) {
    if (!validateAuctionRequestTime(request)) {
      throw new RuntimeException("Auction timings are invalid");
    }

    if (request.biddingItems() == null || request.biddingItems().isEmpty()) {
      throw new RuntimeException("Auction should have at least one bidding item");
    }

    if (!isValidPickupLocation(request.pickupLocation())) {
      throw new RuntimeException("Auction should have a pickup location with all fields provided");
    }
  }

  private static boolean isValidPickupLocation(PickupLocationRequest location) {
    return location != null
        && location.unit() != null
        && isNotBlank(location.street())
        && isNotBlank(location.city())
        && isNotBlank(location.postalCode())
        && isNotBlank(location.state())
        && isNotBlank(location.country());
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static boolean validateAuctionRequestTime(AuctionRequest request) {
    if (!request.startTime().isBefore(request.endTime())) {
      return false;
    }

    if (isFutureDated(request.startTime())) {
      return isOnQuarterHour(request.endTime());
    }

    return isOnQuarterHour(request.startTime()) && isOnQuarterHour(request.endTime());
  }

  private static boolean isOnQuarterHour(Instant dateTime) {
    return dateTime.equals(dateTime.truncatedTo(ChronoUnit.MINUTES))
        && dateTime.getEpochSecond() % 900 == 0;
  }

  private static boolean isFutureDated(Instant auctionStartTime) {
    return auctionStartTime.isAfter(Instant.now());
  }

  private String generateAuctionUrl(UUID auctionId) {
    return clientUrl + "/auctions/" + auctionId;
  }
}
