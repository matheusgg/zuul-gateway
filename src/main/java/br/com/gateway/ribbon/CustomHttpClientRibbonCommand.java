package br.com.gateway.ribbon;

import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.client.ClientException;

public class CustomHttpClientRibbonCommand extends HttpClientRibbonCommand {

	public CustomHttpClientRibbonCommand(final String commandKey, final RibbonLoadBalancingHttpClient client, final RibbonCommandContext context, final ZuulProperties zuulProperties, final ZuulFallbackProvider zuulFallbackProvider) {
		super(commandKey, client, context, zuulProperties, zuulFallbackProvider);
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		final ClientHttpResponse response = super.run();
		final HttpStatus statusCode = response.getStatusCode();
		if (statusCode.is5xxServerError()) {
			throw new ClientException(statusCode.value(), statusCode.getReasonPhrase());
		}
		return response;
	}
}
