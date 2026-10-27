package com.bidder.application.configs.security;

import java.util.List;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class WhitelistProperties {

  @Bean
  public List<String> whitelistEndpoints(Environment environment) {
    return Binder.get(environment)
        .bind("whitelist-endpoints", Bindable.listOf(String.class))
        .orElse(List.of());
  }
}
