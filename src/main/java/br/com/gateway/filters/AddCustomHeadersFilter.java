package br.com.gateway.filters;

import br.com.gateway.properties.ZuulGatewayProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Existem 4 tipos de filtros no Zuul: pre, routing, post e error.
 * Para registrar um filtro basta que ele extenda ZuulFilter
 * e seja um bean no contexto do Spring.
 */
@Component
public class AddCustomHeadersFilter extends ZuulFilter {

	private final Map<String, String> customHeaders;

	@Autowired
	public AddCustomHeadersFilter(final ZuulGatewayProperties properties) {
		this.customHeaders = properties.getCustomHeaders();
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

	@Override
	public boolean shouldFilter() {
		return this.customHeaders != null && !this.customHeaders.isEmpty();
	}

	@Override
	public Object run() {
		final RequestContext context = RequestContext.getCurrentContext();
		this.customHeaders.forEach(context::addZuulRequestHeader);
		return true;
	}
}
