package br.com.gateway.filters;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import br.com.gateway.properties.ZuulGatewayProperties;
import br.com.gateway.properties.ZuulGatewayProperties.FallbackRoute;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class CustomRibbonRoutingFilter extends RibbonRoutingFilter {

	private static final String FALLBACK_MESSAGE = "Service %s was called and returned status code %s. Calling %s as external fallback...";
	private static final Integer SC_INTERNAL_SERVER_ERROR = 500;
	private final ZuulGatewayProperties properties;

	public CustomRibbonRoutingFilter(final ProxyRequestHelper helper, final RibbonCommandFactory<?> ribbonCommandFactory, final ZuulGatewayProperties properties) {
		super(helper, ribbonCommandFactory, new ArrayList<>());
		this.properties = properties;
	}

	@Override
	public Object run() {
		final RequestContext context = RequestContext.getCurrentContext();
		final String serviceId = (String) context.get("serviceId");
		final FallbackRoute fallbackRoute = this.properties.getFallback().get(serviceId);
		ClientHttpResponse response = (ClientHttpResponse) super.run();
		try {
			final Integer statusCode = this.getStatusCode(context, response);
			if (fallbackRoute != null && SC_INTERNAL_SERVER_ERROR.equals(statusCode)) {
				final Exception originalException = this.removeFirstRequestHeaders(context);
				log.warn(format(FALLBACK_MESSAGE, serviceId, statusCode, fallbackRoute.getServiceId()), originalException);
				context.set("serviceId", fallbackRoute.getServiceId());
				final RibbonCommandContext commandContext = super.buildCommandContext(context);
				response = super.forward(commandContext);
				super.setResponse(response);
			}
		} catch (final ZuulException ex) {
			this.handleException(context, ex, ex.nStatusCode, ex.errorCause);
		} catch (final Exception ex) {
			this.handleException(context, ex, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		return response;
	}

	private int getStatusCode(final RequestContext context, final ClientHttpResponse response) throws IOException {
		Integer statusCode = (Integer) context.get("error.status_code");
		if (statusCode == null && response != null) {
			statusCode = response.getStatusCode().value();
		}
		return statusCode;
	}

	private Exception removeFirstRequestHeaders(final RequestContext context) {
		context.remove("error.status_code");
		context.remove("error.message");
		context.remove("originResponseHeaders");
		context.remove("zuulResponseHeaders");
		return (Exception) context.remove("error.exception");
	}

	private void handleException(final RequestContext context, final Exception ex, final int statusCode, final String message) {
		log.info(ex.getMessage(), ex);
		context.set("fallback.error.exception", ex);
		context.set("fallback.error.status_code", statusCode);
		context.set("fallback.error.message", message);
	}
}
