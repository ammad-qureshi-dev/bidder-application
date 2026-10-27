package com.bidder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidder.application.models.entities.auth.AppUser;
import com.bidder.application.repositories.catalog.ItemRepository;
import com.bidder.application.services.auth.JwtService;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the request path: JWT security + path prefixes, the notification proxy, and the bidding
 * <-> catalog calls that used to go over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Import(TestcontainersConfiguration.class)
class MonolithIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @Autowired private ItemRepository itemRepository;

  @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;

  // stands in for notification-service; records the X-App-User-Id it receives
  private static final AtomicReference<String> notificationUserIdHeader = new AtomicReference<>();

  private static final HttpServer notificationService = startNotificationStub();

  @DynamicPropertySource
  static void notificationRoute(DynamicPropertyRegistry registry) {
    registry.add(
        "routes.notification",
        () -> "http://localhost:" + notificationService.getAddress().getPort());
  }

  @AfterAll
  static void stopNotificationStub() {
    notificationService.stop(0);
  }

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/api/v1/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void whitelistedServiceEndpointIsReachableWithoutAuth() throws Exception {
    mockMvc
        .perform(get("/catalog/api/v1/auction/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @Test
  void protectedEndpointRequiresJwtCookie() throws Exception {
    mockMvc.perform(get("/bidding/api/v1/bid/search/my-bids")).andExpect(status().isUnauthorized());
  }

  @Test
  void controllersAreOnlyServedUnderGatewayPrefix() throws Exception {
    // /api/v1/item/** was the service-internal path; publicly it was only /catalog/api/v1/item/**
    mockMvc.perform(get("/api/v1/item/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedEndpointReadsUserFromJwt() throws Exception {
    mockMvc
        .perform(get("/bidding/api/v1/bid/search/my-bids").cookie(tokenFor(UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void accountVerificationEndpointsRequireLogin() throws Exception {
    mockMvc
        .perform(get("/identity-and-auth/api/v1/auth/account-verification/is-verified"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/identity-and-auth/api/v1/auth/account-verification/send-link"))
        .andExpect(status().isUnauthorized());
    // the rest of /auth/** stays public
    mockMvc.perform(post("/identity-and-auth/api/v1/auth/logout")).andExpect(status().isOk());
  }

  @Test
  void notificationProxyForwardsUserIdFromJwt() throws Exception {
    var appUserId = UUID.randomUUID();

    mockMvc
        .perform(get("/notification/api/v1/notification/inbox"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            get("/notification/api/v1/notification/inbox")
                .cookie(tokenFor(appUserId))
                .header("X-App-User-Id", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
    assertThat(notificationUserIdHeader.get()).isEqualTo(appUserId.toString());
  }

  @Test
  void placingBidUpdatesCatalogItemInProcess() throws Exception {
    var ownerId = UUID.randomUUID();
    var bidderId = UUID.randomUUID();
    var start = Instant.now().truncatedTo(ChronoUnit.HOURS);
    var end = start.plus(1, ChronoUnit.DAYS);

    var auctionJson =
        """
				{
				  "title": "Monolith test auction",
				  "startTime": "%s",
				  "endTime": "%s",
				  "biddingItems": [{"title": "Lamp", "description": "desk lamp", "minimumPrice": 10}],
				  "categories": ["OTHER"],
				  "pickupLocation": {"unit": 1, "street": "1 Main St", "city": "Toronto", "postalCode": "M1M1M1",
				                     "state": "ON", "country": "CA"}
				}
				"""
            .formatted(start, end);

    var createAuction =
        mockMvc
            .perform(
                post("/catalog/api/v1/auction")
                    .cookie(tokenFor(ownerId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(auctionJson))
            .andExpect(status().isCreated())
            .andReturn();
    String auctionId = JsonPath.read(createAuction.getResponse().getContentAsString(), "$.data");

    var viewAuction =
        mockMvc
            .perform(get("/catalog/api/v1/auction/view/" + auctionId))
            .andExpect(status().isOk())
            .andReturn();
    String itemId =
        JsonPath.read(viewAuction.getResponse().getContentAsString(), "$.data.items[0].id");

    var bidJson =
        """
				{"auctionId": "%s", "itemId": "%s", "amount": 25, "itemName": "Lamp"}
				"""
            .formatted(auctionId, itemId);

    var createBid =
        mockMvc
            .perform(
                post("/bidding/api/v1/bid")
                    .cookie(tokenFor(bidderId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bidJson))
            .andExpect(status().isCreated())
            .andReturn();
    String bidId = JsonPath.read(createBid.getResponse().getContentAsString(), "$.data");

    // bidding-service -> catalog-service (in-process) set the item's highest bid
    mockMvc
        .perform(get("/catalog/api/v1/item/view/" + itemId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.highestBidId").value(bidId))
        .andExpect(jsonPath("$.data.highestBidAmount").value(25));

    var bids =
        mockMvc
            .perform(get("/bidding/api/v1/bid/search").param("bidIds", bidId))
            .andExpect(status().isOk())
            .andReturn();
    String bidStatus = JsonPath.read(bids.getResponse().getContentAsString(), "$.data[0].status");
    assertThat(bidStatus).isEqualTo("ACTIVE");

    // the caller comes from the JWT; a spoofed X-App-User-Id header is ignored
    mockMvc
        .perform(
            get("/bidding/api/v1/bid/search/my-bids")
                .cookie(tokenFor(UUID.randomUUID()))
                .header("X-App-User-Id", bidderId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
    mockMvc
        .perform(get("/bidding/api/v1/bid/search/my-bids").cookie(tokenFor(bidderId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(bidId));

    // events still go to the notification topic consumed by notification-service
    verify(kafkaTemplate, atLeastOnce()).send(eq("notification"), any());

    // closing the auction marks the highest bid as the winner
    mockMvc
        .perform(
            put("/catalog/api/v1/auction/" + auctionId + "/status/CLOSED")
                .cookie(tokenFor(ownerId)))
        .andExpect(status().isOk());

    var closedBids =
        mockMvc
            .perform(get("/bidding/api/v1/bid/search").param("bidIds", bidId))
            .andExpect(status().isOk())
            .andReturn();
    String closedBidStatus =
        JsonPath.read(closedBids.getResponse().getContentAsString(), "$.data[0].status");
    assertThat(closedBidStatus).isEqualTo("WINNER");
  }

  @Test
  void itemViewsAreCountedExceptForTheOwner() throws Exception {
    var ownerId = UUID.randomUUID();
    var itemId = createAuctionWithItem(ownerId)[1];
    var viewUrl = "/catalog/api/v1/item/view/" + itemId;

    // the response count includes the current view
    mockMvc.perform(get(viewUrl)).andExpect(jsonPath("$.data.viewCount").value(1)); // anonymous
    mockMvc
        .perform(get(viewUrl).cookie(tokenFor(UUID.randomUUID())))
        .andExpect(jsonPath("$.data.viewCount").value(2)); // other user
    mockMvc
        .perform(get(viewUrl).cookie(new Cookie("token", "not-a-jwt")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.viewCount").value(3)); // bad cookie = anonymous
    mockMvc
        .perform(get(viewUrl).cookie(tokenFor(ownerId)))
        .andExpect(jsonPath("$.data.viewCount").value(3)); // owner
    mockMvc
        .perform(get(viewUrl).cookie(tokenFor(ownerId)))
        .andExpect(jsonPath("$.data.viewCount").value(3)); // owner

    var item = itemRepository.findById(UUID.fromString(itemId)).orElseThrow();
    assertThat(item.getItemMetadata().getViewCount()).isEqualTo(3);
    assertThat(item.getVersion())
        .isZero(); // view counting must not bump the optimistic lock version
  }

  @Test
  void auctionViewsAreCountedExceptForTheOwner() throws Exception {
    var ownerId = UUID.randomUUID();
    var ids = createAuctionWithItem(ownerId); // its anonymous GET of the auction is view #1
    var viewUrl = "/catalog/api/v1/auction/view/" + ids[0];

    mockMvc
        .perform(get(viewUrl).cookie(tokenFor(ownerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.viewCount").value(1)); // owner, not counted
    mockMvc
        .perform(get(viewUrl).cookie(tokenFor(UUID.randomUUID())))
        .andExpect(jsonPath("$.data.viewCount").value(2)); // other user
    mockMvc
        .perform(get(viewUrl))
        .andExpect(jsonPath("$.data.viewCount").value(3)) // anonymous
        // viewing the auction doesn't count as viewing its items
        .andExpect(jsonPath("$.data.items[0].viewCount").value(0));
  }

  @Test
  void detailsEndpointsArePublicAndDontCountViews() throws Exception {
    var ids =
        createAuctionWithItem(UUID.randomUUID()); // its anonymous GET of the auction is view #1
    var auctionDetails = "/catalog/api/v1/auction/" + ids[0] + "/details";
    var itemDetails = "/catalog/api/v1/item/" + ids[1] + "/details";

    for (int i = 0; i < 2; i++) {
      mockMvc
          .perform(get(auctionDetails))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(ids[0]))
          .andExpect(jsonPath("$.data.viewCount").value(1));
      mockMvc
          .perform(get(itemDetails).cookie(tokenFor(UUID.randomUUID())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(ids[1]))
          .andExpect(jsonPath("$.data.viewCount").value(0));
    }

    mockMvc
        .perform(get("/catalog/api/v1/item/" + UUID.randomUUID() + "/details"))
        .andExpect(status().isNotFound());
  }

  /**
   * @return {auctionId, itemId}
   */
  private String[] createAuctionWithItem(UUID ownerId) throws Exception {
    var start = Instant.now().truncatedTo(ChronoUnit.HOURS);
    var auctionJson =
        """
				{
				  "title": "View count auction",
				  "startTime": "%s",
				  "endTime": "%s",
				  "biddingItems": [{"title": "Chair", "minimumPrice": 5}],
				  "pickupLocation": {"unit": 1, "street": "1 Main St", "city": "Toronto", "postalCode": "M1M1M1",
				                     "state": "ON", "country": "CA"}
				}
				"""
            .formatted(start, start.plus(1, ChronoUnit.DAYS));

    var created =
        mockMvc
            .perform(
                post("/catalog/api/v1/auction")
                    .cookie(tokenFor(ownerId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(auctionJson))
            .andExpect(status().isCreated())
            .andReturn();
    String auctionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data");

    var auction =
        mockMvc
            .perform(get("/catalog/api/v1/auction/view/" + auctionId))
            .andExpect(status().isOk())
            .andReturn();
    String itemId = JsonPath.read(auction.getResponse().getContentAsString(), "$.data.items[0].id");
    return new String[] {auctionId, itemId};
  }

  private static HttpServer startNotificationStub() {
    try {
      var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/api/v1/notification",
          exchange -> {
            notificationUserIdHeader.set(exchange.getRequestHeaders().getFirst("X-App-User-Id"));
            var body = "{\"data\":[]}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Cookie tokenFor(UUID appUserId) {
    var token =
        jwtService.generateToken(
            new AppUser(appUserId, appUserId + "@test.bidder.app", null, null));
    return new Cookie("token", token);
  }
}
