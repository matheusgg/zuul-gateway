package br.com.gateway.fallback;

import java.util.HashSet;
import java.util.Set;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.gateway.properties.ZuulGatewayProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CustomZuulFallbackConfig {

	private final ZuulGatewayProperties properties;
	private final ProxyRequestHelper helper;
	private final SpringClientFactory clientFactory;
	private final ZuulProperties zuulProperties;

	@Bean
	public Set<ZuulFallbackProvider> createFallbackProviders() {
		final Set<ZuulFallbackProvider> providers = new HashSet<>();
		this.properties.getFallback().forEach((serviceId, fallbackRoute) -> providers.add(CustomZuulFallbackProvider.builder()
				.serviceId(serviceId)
				.fallbackRoute(fallbackRoute)
				.helper(this.helper)
				.clientFactory(this.clientFactory)
				.zuulProperties(this.zuulProperties)
				.build()));
		return providers;
	}
}
