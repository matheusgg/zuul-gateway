package br.com.gateway.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("br.com.gateway")
public class ZuulGatewayProperties {

	private Map<String, String> customHeaders;
	private Map<String, FallbackRoute> fallback;

	@Data
	public static class FallbackRoute {
		private String serviceId;
		private String url;
	}

}
