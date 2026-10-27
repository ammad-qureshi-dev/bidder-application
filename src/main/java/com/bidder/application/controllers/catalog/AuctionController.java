/* (C) 2026
bidder.app */
package com.bidder.application.controllers.catalog;

import com.bidder.application.controllers.dtos.request.catalog.AuctionRequest;
import com.bidder.application.controllers.dtos.response.catalog.AuctionResponse;
import com.bidder.application.controllers.dtos.response.catalog.AuctionSummaryResponse;
import com.bidder.application.controllers.dtos.response.catalog.ItemSummaryResponse;
import com.bidder.application.controllers.dtos.response.catalog.PageResponse;
import com.bidder.application.controllers.message.ApiResponse;
import com.bidder.application.models.entities.auth.AppUser;
import com.bidder.application.models.enums.catalog.AuctionStatus;
import com.bidder.application.services.catalog.AuctionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auction")
@RequiredArgsConstructor
public class AuctionController {

  private final AuctionService auctionService;

  // ToDo: [SECURITY CONCERN] -- @RequestBody is not annotated with @Valid, so the
  // @NotNull constraints declared
  // on AuctionRequest are never enforced, allowing malformed/null input to reach
  // the service layer.
  @PostMapping
  public ResponseEntity<ApiResponse<UUID>> createAuction(
      @RequestBody AuctionRequest request,
      @AuthenticationPrincipal(expression = "id") UUID appUserId) {
    var auctionId = auctionService.createAuction(request, appUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.<UUID>builder().data(auctionId).build());
  }

  // ToDo: [SECURITY CONCERN] -- no ownership/authorization check: this endpoint
  // does not verify that the caller
  // is the auction's owner before applying the update. Combined with no
  // authentication, any user can edit any
  // other user's auction (IDOR).
  @PutMapping("/{auctionId}")
  public ResponseEntity<ApiResponse<UUID>> updateAuction(
      @PathVariable UUID auctionId, @RequestBody AuctionRequest request) {
    var response = auctionService.updateAuction(auctionId, request);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<UUID>builder().data(response).build());
  }

  // ToDo: [SECURITY CONCERN] -- no ownership/authorization check: any caller can
  // transition any auction's status
  // (e.g. force-close, pause, or re-open) regardless of whether they own it,
  // since there is no check against the
  // caller's identity here.
  @PutMapping("/{auctionId}/status/{status}")
  public ResponseEntity<ApiResponse<UUID>> updateAuctionStatus(
      @PathVariable UUID auctionId, @PathVariable AuctionStatus status) {
    auctionService.updateAuctionStatus(auctionId, status);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<UUID>builder().data(auctionId).build());
  }

  @GetMapping("/view/{auctionId}")
  public ResponseEntity<ApiResponse<AuctionResponse>> getAuction(
      @PathVariable UUID auctionId, @AuthenticationPrincipal AppUser appUser) {
    // public endpoint: appUser is null when the viewer isn't logged in
    var viewerId = appUser != null ? appUser.getId() : null;
    var response = auctionService.viewAuction(auctionId, viewerId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<AuctionResponse>builder().data(response).build());
  }

  // same data as /view/{auctionId} without counting a view: for lookups that aren't someone opening
  // the auction page (e.g. the item page header, the edit form, refreshing after a status change)
  @GetMapping("/{auctionId}/details")
  public ResponseEntity<ApiResponse<AuctionResponse>> getAuctionDetails(
      @PathVariable UUID auctionId) {
    var response = auctionService.getAuctionResponse(auctionId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<AuctionResponse>builder().data(response).build());
  }

  // ToDo: paginated
  @GetMapping("/{auctionId}/items")
  public ResponseEntity<ApiResponse<List<ItemSummaryResponse>>> getAuctionItems(
      @PathVariable UUID auctionId) {
    var response = auctionService.getItemsInAuction(auctionId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<List<ItemSummaryResponse>>builder().data(response).build());
  }

  @GetMapping("/my-auctions")
  public ResponseEntity<ApiResponse<PageResponse<AuctionSummaryResponse>>> getMyAuctions(
      @AuthenticationPrincipal(expression = "id") UUID appUserId,
      @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    var response = auctionService.getMyAuctions(appUserId, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<PageResponse<AuctionSummaryResponse>>builder().data(response).build());
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<PageResponse<AuctionSummaryResponse>>> searchAuctions(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) AuctionStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startAfter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endBefore,
      @PageableDefault(size = 25, sort = "startTime", direction = Sort.Direction.ASC)
          Pageable pageable) {
    var results = auctionService.searchAuctions(title, status, startAfter, endBefore, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.<PageResponse<AuctionSummaryResponse>>builder().data(results).build());
  }
}
