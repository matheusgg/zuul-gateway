package br.com.gateway.ribbon;

import static java.util.Collections.emptySet;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommandFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomHttpClientRibbonCommandFactory extends AbstractRibbonCommandFactory {

	private final SpringClientFactory clientFactory;
	private final ZuulProperties zuulProperties;

	public CustomHttpClientRibbonCommandFactory(final SpringClientFactory clientFactory, final ZuulProperties zuulProperties) {
		super(emptySet());
		this.clientFactory = clientFactory;
		this.zuulProperties = zuulProperties;
	}

	@Override
	public HttpClientRibbonCommand create(final RibbonCommandContext context) {
		final ZuulFallbackProvider zuulFallbackProvider = super.getFallbackProvider(context.getServiceId());
		final String serviceId = context.getServiceId();
		final RibbonLoadBalancingHttpClient client = this.clientFactory.getClient(serviceId, RibbonLoadBalancingHttpClient.class);
		client.setLoadBalancer(this.clientFactory.getLoadBalancer(serviceId));
		return new CustomHttpClientRibbonCommand(serviceId, client, context, this.zuulProperties, zuulFallbackProvider);
	}
}
