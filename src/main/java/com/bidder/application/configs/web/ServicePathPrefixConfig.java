package com.bidder.application.configs.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Keeps the public URLs the frontend already uses (and that the whitelist
 * endpoints reference): /identity-and-auth/**, /catalog/** and /bidding/**.
 */
@Configuration
public class ServicePathPrefixConfig implements WebMvcConfigurer {

	private static final String CONTROLLERS = "com.bidder.application.controllers";

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix("/identity-and-auth", HandlerTypePredicate.forBasePackage(CONTROLLERS + ".auth"));
		configurer.addPathPrefix("/catalog", HandlerTypePredicate.forBasePackage(CONTROLLERS + ".catalog"));
		configurer.addPathPrefix("/bidding", HandlerTypePredicate.forBasePackage(CONTROLLERS + ".bids"));
	}
}
