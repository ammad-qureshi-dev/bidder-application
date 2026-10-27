/* (C) 2026
bidder.app */
package com.bidder.application.controllers.catalog;

import com.bidder.application.controllers.dtos.request.catalog.AcceptingBidsRequest;
import com.bidder.application.controllers.dtos.request.catalog.UpdatedBidRequest;
import com.bidder.application.controllers.dtos.response.catalog.ItemResponse;
import com.bidder.application.controllers.message.ApiResponse;
import com.bidder.application.models.entities.auth.AppUser;
import com.bidder.application.services.catalog.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/item")
@RequiredArgsConstructor
public class ItemController {

  private final ItemService itemService;

  @GetMapping("/view/{itemId}")
  public ResponseEntity<ApiResponse<ItemResponse>> getItem(
      @PathVariable UUID itemId, @AuthenticationPrincipal AppUser appUser) {
    // public endpoint: appUser is null when the viewer isn't logged in
    var viewerId = appUser != null ? appUser.getId() : null;
    var item = itemService.viewItem(itemId, viewerId);
    return ResponseEntity.ok().body(ApiResponse.<ItemResponse>builder().data(item).build());
  }

  // same data as /view/{itemId} without counting a view: for lookups that aren't someone opening
  // the item page (e.g. titles on My Bids, refreshing after a bid)
  @GetMapping("/{itemId}/details")
  public ResponseEntity<ApiResponse<ItemResponse>> getItemDetails(@PathVariable UUID itemId) {
    var item =
        itemService
            .findItem(itemId)
            .orElseThrow(() -> new NoSuchElementException("Item not found"));
    return ResponseEntity.ok().body(ApiResponse.<ItemResponse>builder().data(item).build());
  }

  @PutMapping("/{itemId}/accepting-bids")
  public ResponseEntity<ApiResponse<?>> acceptingBids(
      @PathVariable UUID itemId, @RequestBody AcceptingBidsRequest request) {
    return ResponseEntity.ok()
        .body(
            ApiResponse.<Boolean>builder()
                .data(itemService.acceptingBids(itemId, request))
                .build());
  }

  // ToDo: [SECURITY CONCERN] -- bidderId, bidId, and amount in UpdatedBidRequest
  // are entirely client-supplied
  // and are never cross-checked against bidding-service (the system of record for
  // bids). Combined with no
  // authentication, any caller can forge a winning bid for an item with an
  // arbitrary bidId/bidderId/amount
  // without ever having placed a real bid.
  // ToDo: [SECURITY CONCERN] -- @RequestBody is not annotated with @Valid, so the
  // @NotNull constraints declared
  // on UpdatedBidRequest are never enforced.
  @Operation(
      description =
          "Updates the item's highest bid with the provided request. Returns the old, outbid bid")
  @PutMapping("/{itemId}/update-highest-bid")
  public ResponseEntity<ApiResponse<UUID>> updateHighestBid(
      @PathVariable UUID itemId, @RequestBody UpdatedBidRequest request) {
    var outbidBidId = itemService.updateHighestBidForItem(itemId, request);
    return ResponseEntity.ok().body(ApiResponse.<UUID>builder().data(outbidBidId).build());
  }
}
