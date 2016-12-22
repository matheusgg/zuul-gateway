package br.com.gateway.fallback;

import java.util.HashSet;
import java.util.Set;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.gateway.properties.ZuulGatewayProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CustomZuulFallbackConfig {

	private final ZuulGatewayProperties properties;
	private final ApplicationContext context;
	private final ProxyRequestHelper helper;

	@Bean
	public Set<ZuulFallbackProvider> createFallbackProviders() {
		final Set<ZuulFallbackProvider> providers = new HashSet<>();
		this.properties.getFallback().forEach((serviceId, fallbackRoute) -> providers.add(CustomZuulFallbackProvider.builder()
				.serviceId(serviceId)
				.fallbackRoute(fallbackRoute)
				.helper(this.helper)
				.applicationContext(this.context)
				.build()));
		return providers;
	}
}
