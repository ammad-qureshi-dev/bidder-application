package com.bidder.application.configs.notification;

import com.bidder.application.models.entities.auth.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * notification-service is a separate deployment with no auth of its own. The
 * frontend reaches it through /notification/**, which is authenticated here
 * (SecurityConfig) and forwarded with the caller's id in X-App-User-Id, the
 * header notification-service identifies users by.
 */
@Configuration
public class NotificationRouteConfig {

    public static final String APP_USER_HEADER = "X-App-User-Id";

    @Value("${routes.notification}")
    private String notification;

    @Bean
    public RouterFunction<ServerResponse> notificationRoute() {
        return route("notification-service")
                .route(path("/notification/**"), http())
                .before(NotificationRouteConfig::withAppUserIdFromJwt)
                .filter(stripPrefix(1))
                .filter(uri(notification))
                .build();
    }

    // any client-supplied X-App-User-Id is replaced, so callers can't act as another user
    private static ServerRequest withAppUserIdFromJwt(ServerRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return ServerRequest.from(request).headers(headers -> {
            headers.remove(APP_USER_HEADER);
            if (authentication != null && authentication.getPrincipal() instanceof AppUser appUser) {
                headers.set(APP_USER_HEADER, appUser.getId().toString());
            }
        }).build();
    }
}
