package br.com.gateway.fallback;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.zuul.context.RequestContext;

import br.com.gateway.properties.ZuulGatewayProperties.FallbackRoute;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class CustomZuulFallbackProvider implements ZuulFallbackProvider {

	private static final String FALLBACK_MESSAGE = "Service %s was called but returned an error. Calling %s as external fallback...";
	private final String serviceId;
	private final FallbackRoute fallbackRoute;
	private final ProxyRequestHelper helper;
	private final SpringClientFactory clientFactory;
	private final ZuulProperties zuulProperties;

	@Override
	public String getRoute() {
		return this.serviceId;
	}

	@Override
	public ClientHttpResponse fallbackResponse() {
		ClientHttpResponse response = null;

		try {
			log.warn(format(FALLBACK_MESSAGE, this.serviceId, this.fallbackRoute.getServiceId()));

			final RequestContext context = RequestContext.getCurrentContext();
			final Optional<String> url = Optional.ofNullable(this.fallbackRoute.getUrl());

			if (url.isPresent()) {
				context.setRouteHost(new URL(url.get()));
				response = new EmptyClientHttpResponse();
			} else {
				final RibbonCommandContext commandContext = this.buildCommandContext(context);
				final RibbonCommand command = this.buildRibbonCommand(commandContext);
				response = command.execute();
			}

		} catch (final Exception e) {
			log.error("Error during fallbackResponse", e);
			throw new RuntimeException(e);
		}

		return response;
	}

	private RibbonCommandContext buildCommandContext(final RequestContext context) throws MalformedURLException {
		final HttpServletRequest request = context.getRequest();
		final long contentLength = request.getContentLengthLong();
		final Boolean retryable = (Boolean) context.get("retryable");
		final String uri = this.helper.buildZuulRequestURI(request).replace("//", "/");
		final MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
		final MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
		final Optional<String> method = Optional.ofNullable(request.getMethod());

		final InputStream requestEntity = this.getRequestBody(context, request);
		if (contentLength < 0) {
			context.setChunkedRequestBody();
		}

		return new RibbonCommandContext(this.fallbackRoute.getServiceId(), method.orElse("GET"), uri, retryable, headers, params, requestEntity, emptyList(), contentLength);
	}

	private InputStream getRequestBody(final RequestContext context, final HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = (InputStream) context.get("requestEntity");
			if (requestEntity == null) {
				requestEntity = request.getInputStream();
			}
		} catch (final IOException e) {
			log.error("Error during getRequestBody", e);
		}
		return requestEntity;
	}

	private RibbonCommand buildRibbonCommand(final RibbonCommandContext context) {
		final String fallbackServiceId = this.fallbackRoute.getServiceId();
		final RibbonLoadBalancingHttpClient client = this.clientFactory.getClient(fallbackServiceId, RibbonLoadBalancingHttpClient.class);
		client.setLoadBalancer(this.clientFactory.getLoadBalancer(fallbackServiceId));
		return new HttpClientRibbonCommand(fallbackServiceId, client, context, zuulProperties, null);
	}

	static class EmptyClientHttpResponse implements ClientHttpResponse {

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return HttpStatus.NO_CONTENT;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return HttpStatus.NO_CONTENT.value();
		}

		@Override
		public String getStatusText() throws IOException {
			return HttpStatus.NO_CONTENT.getReasonPhrase();
		}

		@Override
		public void close() {

		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		public HttpHeaders getHeaders() {
			return new HttpHeaders();
		}
	}
}
