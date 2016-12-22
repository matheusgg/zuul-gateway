package br.com.gateway.fallback;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.zuul.context.RequestContext;

import br.com.gateway.properties.ZuulGatewayProperties.FallbackRoute;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class CustomZuulFallbackProvider implements ZuulFallbackProvider {

	private String serviceId;
	private FallbackRoute fallbackRoute;
	private RibbonCommandFactory<?> ribbonCommandFactory;
	private ProxyRequestHelper helper;
	private ApplicationContext applicationContext;

	@Override
	public String getRoute() {
		return this.serviceId;
	}

	@Override
	public ClientHttpResponse fallbackResponse() {
		try {
			this.loadRibbonCommandFactory();
			final RequestContext context = RequestContext.getCurrentContext();
			final RibbonCommandContext commandContext = this.buildCommandContext(context);
			final RibbonCommand command = this.ribbonCommandFactory.create(commandContext);
			return command.execute();
		} catch (final Exception e) {
			log.error("Error during fallbackResponse", e);
			throw new RuntimeException(e);
		}
	}

	private void loadRibbonCommandFactory() {
		if (this.ribbonCommandFactory == null) {
			this.ribbonCommandFactory = this.applicationContext.getBean(RibbonCommandFactory.class);
		}
	}

	private RibbonCommandContext buildCommandContext(final RequestContext context) {
		final HttpServletRequest request = context.getRequest();

		final MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
		final MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
		final Optional<String> method = Optional.ofNullable(request.getMethod());

		final InputStream requestEntity = this.getRequestBody(context, request);
		if (request.getContentLength() < 0) {
			context.setChunkedRequestBody();
		}

		final Boolean retryable = (Boolean) context.get("retryable");
		final String uri = this.helper.buildZuulRequestURI(request).replace("//", "/");
		final long contentLength = request.getContentLength();

		return new RibbonCommandContext(this.serviceId, method.orElse("GET"), uri, retryable, headers, params, requestEntity, emptyList(), contentLength);
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
}
