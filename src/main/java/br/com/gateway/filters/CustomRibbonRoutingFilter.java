package br.com.gateway.filters;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.netflix.client.ClientException;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import br.com.gateway.properties.ZuulGatewayProperties;
import br.com.gateway.properties.ZuulGatewayProperties.FallbackRoute;
import br.com.gateway.ribbon.CustomHttpClientRibbonCommandFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomRibbonRoutingFilter extends RibbonRoutingFilter {

	private static final String FALLBACK_MESSAGE = "Service %s was called and returned status code %s. Calling %s as external fallback...";
	private static final Integer SC_INTERNAL_SERVER_ERROR = 500;
	private final ZuulGatewayProperties properties;

	public CustomRibbonRoutingFilter(final ProxyRequestHelper helper, final CustomHttpClientRibbonCommandFactory commandFactory, final ZuulGatewayProperties properties) {
		super(helper, commandFactory, emptyList());
		this.properties = properties;
	}

	@Override
	public Object run() {
		super.run();
		final RequestContext context = RequestContext.getCurrentContext();
		final String serviceId = (String) context.get("serviceId");
		final FallbackRoute fallbackRoute = this.properties.getFallback().get(serviceId);
		try {
			final Integer statusCode = this.getStatusCode(context);
			if (fallbackRoute != null && SC_INTERNAL_SERVER_ERROR.equals(statusCode)) {
				final String fallbackServiceId = fallbackRoute.getServiceId();
				final String fallbackUrl = fallbackRoute.getUrl();
				final Exception ex = this.removeFirstResponseHeaders(context);
				log.warn(format(FALLBACK_MESSAGE, serviceId, statusCode, ofNullable(fallbackServiceId).orElse(fallbackUrl)), ex);
				if (fallbackUrl != null) {
					this.forward(context, fallbackUrl);
				} else {
					this.forwardUsingLoadBalance(context, fallbackServiceId);
				}
			}
		} catch (final ZuulException ex) {
			final ClientException clientException = super.findClientException(ex);
			if (clientException != null) {
				this.handleException(context, clientException, clientException.getErrorCode(), clientException.getMessage());
			} else {
				this.handleException(context, ex, ex.nStatusCode, ex.errorCause);
			}
		} catch (final Exception ex) {
			this.handleException(context, ex, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		return null;
	}

	private int getStatusCode(final RequestContext context) throws IOException {
		Integer statusCode = null;
		final RibbonApacheHttpResponse response = (RibbonApacheHttpResponse) context.get("ribbonResponse");
		if (response != null) {
			statusCode = response.getStatus();
		} else {
			statusCode = (Integer) context.get("error.status_code");
		}
		return statusCode;
	}

	private Exception removeFirstResponseHeaders(final RequestContext context) {
		context.remove("error.status_code");
		context.remove("error.message");
		context.remove("originResponseHeaders");
		context.remove("zuulResponseHeaders");
		return (Exception) context.remove("error.exception");
	}

	private void forward(final RequestContext context, final String url) throws Exception {
		context.setRouteHost(new URL(url));
		super.setResponse(new EmptyClientHttpResponse());
	}

	private void forwardUsingLoadBalance(final RequestContext context, final String serviceId) throws Exception {
		context.set("serviceId", serviceId);
		final RibbonCommandContext commandContext = super.buildCommandContext(context);
		final ClientHttpResponse response = super.forward(commandContext);
		super.setResponse(response);
	}

	private void handleException(final RequestContext context, final Exception ex, final int statusCode, final String message) {
		log.info(ex.getMessage(), ex);
		context.set("error.exception", ex);
		context.set("error.status_code", statusCode);
		context.set("error.message", message);
	}

	static class EmptyClientHttpResponse implements ClientHttpResponse {

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return NO_CONTENT;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return NO_CONTENT.value();
		}

		@Override
		public String getStatusText() throws IOException {
			return NO_CONTENT.getReasonPhrase();
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
