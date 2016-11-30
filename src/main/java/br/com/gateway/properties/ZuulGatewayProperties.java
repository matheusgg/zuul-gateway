package br.com.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties("br.com.gateway")
public class ZuulGatewayProperties {

	private Map<String, String> customHeaders;

}
