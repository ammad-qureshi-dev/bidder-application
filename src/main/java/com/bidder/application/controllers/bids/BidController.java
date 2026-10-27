/* (C) 2026
bidder.app */
package com.bidder.application.controllers.bids;

import com.bidder.application.controllers.dtos.request.bids.BidRejectRequest;
import com.bidder.application.controllers.dtos.request.bids.BidRequest;
import com.bidder.application.controllers.dtos.response.bids.BidSummaryResponse;
import com.bidder.application.controllers.message.ApiResponse;
import com.bidder.application.models.enums.bids.BidStatus;
import com.bidder.application.services.bids.BidService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/bid")
@RequiredArgsConstructor
public class BidController {

  private final BidService bidService;

  @PostMapping
  public ResponseEntity<ApiResponse<UUID>> createBid(
      @AuthenticationPrincipal(expression = "id") UUID bidderId,
      @RequestBody @Valid BidRequest request)
      throws IllegalAccessException {
    var response = bidService.createBid(request, bidderId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.<UUID>builder().data(response).build());
  }

  @PutMapping("/{bidId}")
  public ResponseEntity<ApiResponse<UUID>> updateBid(
      @PathVariable UUID bidId,
      @RequestBody BidRequest request,
      @AuthenticationPrincipal(expression = "id") UUID bidderId)
      throws IllegalAccessException {
    if (!bidService.isBidOwner(bidId, bidderId)) {
      log.error("Cannot update bid - userId={} does not own bid={}", bidderId, bidId);
      throw new IllegalStateException("Cannot update bid - user does not own bid");
    }

    var response = bidService.updateBid(bidId, request, bidderId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<UUID>builder().data(response).build());
  }

  @PostMapping("/accept/{bidId}")
  public ResponseEntity<ApiResponse<Boolean>> acceptBid(
      @PathVariable UUID bidId, @AuthenticationPrincipal(expression = "id") UUID ownerId)
      throws IllegalAccessException {
    bidService.acceptBid(bidId, ownerId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<Boolean>builder().data(true).build());
  }

  @PostMapping("/reject/{bidId}")
  public ResponseEntity<ApiResponse<Boolean>> rejectBid(
      @PathVariable UUID bidId,
      @RequestBody BidRejectRequest request,
      @AuthenticationPrincipal(expression = "id") UUID ownerId)
      throws IllegalAccessException {
    bidService.rejectBid(bidId, ownerId, request);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<Boolean>builder().data(true).build());
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getBidById(
      @RequestParam(required = true) List<UUID> bidIds) {
    var response = bidService.getBids(bidIds);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/search/my-bids")
  public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getMyBids(
      @AuthenticationPrincipal(expression = "id") UUID appUserId) {
    var myBids = bidService.getMyBids(appUserId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<List<BidSummaryResponse>>builder().data(myBids).build());
  }

  @GetMapping("/search/item/{itemId}")
  public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getBidsForItem(
      @PathVariable UUID itemId) {
    var bids = bidService.getBidsForItem(itemId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<List<BidSummaryResponse>>builder().data(bids).build());
  }

  @GetMapping("/item/{itemId}/top-ten-bids")
  public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getTopTenBids(
      @PathVariable UUID itemId) {
    var bids = bidService.getTopTenBidsForItem(itemId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<List<BidSummaryResponse>>builder().data(bids).build());
  }

  @PutMapping("/internal/bulk/update-bids-statuses/{status}")
  public ResponseEntity<ApiResponse<Boolean>> internalBulkUpdateBidsStatuses(
      @RequestParam List<UUID> bidIds, @PathVariable BidStatus status) {
    bidService.internalBulkUpdateBidsStatuses(bidIds, status);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<Boolean>builder().data(true).build());
  }
}
