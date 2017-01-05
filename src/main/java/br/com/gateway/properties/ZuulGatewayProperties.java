package br.com.gateway.properties;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("br.com.gateway")
public class ZuulGatewayProperties implements InitializingBean {

	private Map<String, String> customHeaders;
	private Map<String, FallbackRoute> fallback;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.fallback.forEach((serviceId, route) -> {
			if (isEmpty(route.getServiceId()) && isEmpty(route.getUrl())) {
				throw new IllegalArgumentException("Either serviceId or url must be provided for fallback route " + serviceId);
			}
		});
	}

	@Data
	public static class FallbackRoute {
		private String serviceId;
		private String url;
	}

}
